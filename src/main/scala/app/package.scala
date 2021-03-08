import io.circe.syntax._
import io.circe.{Printer, parser}
import zio.{ZIO, random}
import zio.kafka.serde.Serde

package object app {
  val TopicName = "atopic"
  private val People = Array("Peter", "Trevor", "Alina", "Marek", "Marius")

  def randomPerson =
    random.nextIntBetween(0, People.length).map(People.apply)

  private val commandSerdePrinter = Printer.noSpaces
  val CommandSerde: Serde[Any, Command] = zio.kafka.serde.Serde.string.inmapM(x => ZIO.fromEither(parser.parse(x).flatMap(_.as[Command])))(x => ZIO.succeed(x.asJson.printWith(commandSerdePrinter)))
}
