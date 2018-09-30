package useless

trait PersistentArgument[A] {

  def encode(value: A):      String
  def decode(value: String): A
}

object PersistentArgument {

  @inline def apply[A: PersistentArgument]: PersistentArgument[A] = implicitly[PersistentArgument[A]]
}
