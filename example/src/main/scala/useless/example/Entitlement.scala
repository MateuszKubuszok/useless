package useless.example

import java.util.UUID

final case class Entitlement(userID: UUID, resourceID: UUID, level: Entitlement.Level)

object Entitlement {

  sealed abstract class Level(val name: String) extends Product with Serializable {
    override def toString: String = name
  }
  object Level {
    case object No extends Level("no")
    case object Read extends Level("read")
    case object Write extends Level("write")
    val values = Set(No, Read, Write)
    def findByName(name: String): Level = values.find(_.name equalsIgnoreCase name).getOrElse(No)
  }
}
