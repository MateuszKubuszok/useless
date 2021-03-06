package useless.circe

trait CirceIntegration {

  implicit def persistentArgument[A: io.circe.Decoder: io.circe.Encoder]: useless.PersistentArgument[A] =
    new useless.PersistentArgument[A] {

      def encode(value: A): String = io.circe.Encoder[A].apply(value).noSpaces

      @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
      def decode(value: String): A = io.circe.parser.decode[A](value).right.get
    }
}
