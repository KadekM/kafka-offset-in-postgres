package app

import org.apache.kafka.common.TopicPartition
import skunk.codec.all._
import skunk._
import skunk.implicits._

object Sql {

  final case class TopicPartitionOffset(topic: TopicPartition, offset: Long)

  private def topicPartitionFromString(s: String): Option[TopicPartition] = s.split("-").toList match {
    case t :: o :: Nil =>
      o.toIntOption.map { offset => new TopicPartition(t, offset) }
    case _ => None
  }

  val partitionTableDdl =
    sql"""
                               CREATE TABLE IF NOT EXISTS offsets (
                                 topic VARCHAR PRIMARY KEY,
                                 "offset" BIGINT NOT NULL)""".command

  def selectPartitionSql(topicPartitions: List[String]) = {
    sql"""SELECT topic, "offset" FROM offsets WHERE topic IN (${varchar.list(topicPartitions.size)})
      """.query(varchar ~ int8)
      .map { case t ~ o =>
        topicPartitionFromString(t).map(topic => TopicPartitionOffset(topic, o))
      }
  }

  def updatePartitionCommand(partitions: List[(String, Long)]) = {
    val enc = (varchar ~ int8).values.list(partitions)
    sql"""INSERT INTO offsets (topic, "offset") VALUES $enc ON CONFLICT (topic) DO UPDATE SET "offset"=EXCLUDED."offset"""".command
  }

  final case class Balance(name: String, balance: Int)

  val balanceTableDdl =
    sql"""
                               CREATE TABLE IF NOT EXISTS balances (
                                 name VARCHAR PRIMARY KEY,
                                 balance INT NOT NULL)""".command

  def changeBalanceCommand =
    sql"""
          INSERT INTO balances (name, balance) VALUES ($varchar, $int4) ON CONFLICT (name) DO UPDATE SET balance=EXCLUDED.balance + balances.balance
      """.command

}
