package app

import zio._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.logging.{Logging, log}
import zio.duration._

// produces topic with correct prefix based on args
object CommandProducerApp extends App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val producerSettings = ProducerSettings(List("localhost:9092"))

    val zio = for {
      _ <- log.info("Starting to produce values")
      _ <- Producer.make(producerSettings, Serde.int, CommandSerde).use { p: Producer.Service[Any, Int, Command] =>
        val produce = for {
          amount <- random.nextIntBetween(0, 100)
          from <- randomPerson
          to <- randomPerson
          event <- ZIO.succeed(TransferBalance(from, to, amount))

          _ <- log.info(s"Appending $event")
          _ <- p.produce(TopicName, event.hashCode(), event)
        } yield ()

        (produce *> ZIO.sleep(1.second)).forever
      }
    } yield ()

    val loggingLayer = Logging.console()

    zio
      .provideCustomLayer(loggingLayer)
      .exitCode
  }
}

