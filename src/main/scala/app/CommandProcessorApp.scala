package app

import zio.kafka.consumer._
import zio.kafka.serde.Serde
import zio.logging.{Logging, log}
import cats.effect.{ExitCode => _, _}
import zio._
import zio.interop.catz._
import skunk._
import skunk.implicits._
import natchez.Trace.Implicits.noop
import org.apache.kafka.common.TopicPartition

object CommandProcessorApp extends App {

  type SessionTask = Session[Task]

  def partitionToString(topic: TopicPartition): String = s"${topic.topic()}-${topic.partition()}"

  val dbSessionLayer: Layer[Throwable, Has[SessionTask]] = ZManaged.runtime.flatMap { implicit r: Runtime[Any] =>
    Session.single(
      host = "localhost",
      port = 5432,
      user = "postgres",
      database = "postgres",
      password = Some("mysecretpassword")
    ).toManaged
  }.toLayer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val zio = for {
      session <- ZIO.service[SessionTask]

      _ <- ZIO.runtime[Any].flatMap { implicit r: Runtime[Any] =>
        val manualOffsetRetrieval = Consumer.OffsetRetrieval.Manual { partitions =>
          val list = partitions.map(partitionToString).toList
          val query = Sql.selectPartitionSql(list)
          session.prepare(query).toManaged
            .use(_.stream(list, 64).compile.toVector)
            .map(xs => xs.collect { case Some(t) => t }.map(x => x.topic -> x.offset).toMap)
        }
        val consumerSettings = ConsumerSettings(List("localhost:9092"))
          .withGroupId("my.group")
          .withOffsetRetrieval(manualOffsetRetrieval)
        val consumerM = Consumer.make(consumerSettings)

        for {
          _ <- session.execute(Sql.partitionTableDdl)
          _ <- session.execute(Sql.balanceTableDdl)

          _ <- consumerM.use { consumer =>
            consumer.subscribeAnd(Subscription.topics(TopicName))
              .plainStream(Serde.int, CommandSerde)
              .tap(x => log.info(x.value.toString))
              .foreachChunk { chunk =>
                val offsetBatch = OffsetBatch(chunk.map(_.offset))
                val offsets = offsetBatch.offsets
                val offsetsList = offsets.map { case (topicPartition, offset) => partitionToString(topicPartition) -> offset }.toList
                val commands = chunk.map(_.value)
                session.transaction.toManaged.use { transaction =>
                  for {
                    _ <- ZIO.foreach_(commands) {
                      case TransferBalance(from, to, amount) =>
                        for {
                          _ <- session.prepare(Sql.changeBalanceCommand).toManaged.use(_.execute(from ~ -amount)) // notice the minus sign
                          _ <- session.prepare(Sql.changeBalanceCommand).toManaged.use(_.execute(to ~ amount))
                        } yield ()
                    }
                    _ <- session.prepare(Sql.updatePartitionCommand(offsetsList)).toManaged.use(_.execute(offsetsList))
                    _ <- random.nextInt.flatMap(x => ZIO.fail(new Exception("shit")).when(x % 10 == 0))
                    _ <- transaction.commit
                  } yield ()
                }
              }
          }
        } yield ()
      }
    } yield ()

    val loggingLayer = Logging.console() ++ dbSessionLayer

    zio
      .provideCustomLayer(loggingLayer)
      .exitCode
  }
}
