package useless

sealed abstract class Stage[F[_], I: PersistentArgument, O](implicit val persistentArgument: PersistentArgument[I])
object Stage {

  // serialize A
  private[useless] final case class RetryUntilSucceed[F[_], I: PersistentArgument, O](f: I => F[O])
      extends Stage[F, I, O]
}
