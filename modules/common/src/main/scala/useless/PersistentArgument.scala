package useless

trait PersistentArgument[A] {

  def encode(value: A):      String
  def decode(value: String): A
}

object PersistentArgument {

  @inline def apply[A: PersistentArgument]: PersistentArgument[A] = implicitly[PersistentArgument[A]]

  implicit val persistentUnit: PersistentArgument[Unit] = new PersistentArgument[Unit] {
    def encode(value: Unit):   String = ""
    def decode(value: String): Unit   = ()
  }

  implicit val persistentBoolean: PersistentArgument[Boolean] = new PersistentArgument[Boolean] {
    def encode(value: Boolean): String  = value.toString
    def decode(value: String):  Boolean = value.toBoolean
  }

  implicit val persistentByte: PersistentArgument[Byte] = new PersistentArgument[Byte] {
    def encode(value: Byte):   String = value.toString
    def decode(value: String): Byte   = value.toByte
  }

  implicit val persistentChar: PersistentArgument[Char] = new PersistentArgument[Char] {
    def encode(value: Char):   String = value.toString
    def decode(value: String): Char   = value.head
  }

  implicit val persistentInt: PersistentArgument[Int] = new PersistentArgument[Int] {
    def encode(value: Int):    String = value.toString
    def decode(value: String): Int    = value.toInt
  }

  implicit val persistentShort: PersistentArgument[Short] = new PersistentArgument[Short] {
    def encode(value: Short):  String = value.toString
    def decode(value: String): Short  = value.toShort
  }

  implicit val persistentLong: PersistentArgument[Long] = new PersistentArgument[Long] {
    def encode(value: Long):   String = value.toString
    def decode(value: String): Long   = value.toLong
  }

  implicit val persistentDouble: PersistentArgument[Double] = new PersistentArgument[Double] {
    def encode(value: Double): String = value.toString
    def decode(value: String): Double = value.toDouble
  }

  implicit val persistentFloat: PersistentArgument[Float] = new PersistentArgument[Float] {
    def encode(value: Float):  String = value.toString
    def decode(value: String): Float  = value.toFloat
  }

  implicit val persistentString: PersistentArgument[String] = new PersistentArgument[String] {
    def encode(value: String): String = value
    def decode(value: String): String = value
  }
}
