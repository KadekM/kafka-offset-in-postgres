package app

import io.circe.generic.JsonCodec

@JsonCodec
sealed trait Command

final case class TransferBalance(from: String, to: String, amount: Int) extends Command

