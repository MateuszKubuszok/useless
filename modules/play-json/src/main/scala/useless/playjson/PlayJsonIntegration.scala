package useless.playjson

trait PlayJsonIntegration {

  import play.api.libs.json._

  implicit def persistentArgument[A: Reads: Writes]: useless.PersistentArgument[A] =
    new useless.PersistentArgument[A] {

      def encode(value: A):      String = implicitly[Writes[A]].writes(value).toString
      def decode(value: String): A      = implicitly[Reads[A]].reads(Json.parse(value)).get
    }
}
