import sbt._
import Settings._

lazy val root = project.root
  .setName("useless")
  .setDescription("Build of a simple process manager library")
  .configureRoot
  .noPublish
  .aggregate(core, cats, circe, doobie, playJson, scalaz, slick, example)

lazy val core = project.from("core")
  .setName("useless-core")
  .setDescription("Simple process manager library")
  .setInitialImport("algebras._, algebras.syntax._, internal._")
  .configureModule
  .configureTests()
  .publish
  .settings(Compile / resourceGenerators += task[Seq[File]] {
    val file = (Compile / resourceManaged).value / "useless-version.conf"
    IO.write(file, s"version=${version.value}")
    Seq(file)
  })

lazy val cats = project.from("cats")
  .setName("useless-cats")
  .setDescription("Cats integration for useless")
  .setInitialImport("useless.cats._")
  .configureModule
  .publish
  .dependsOn(core)

lazy val circe = project.from("circe")
  .setName("useless-circe")
  .setDescription("Circe integration for useless")
  .setInitialImport("useless.circe._")
  .configureModule
  .publish
  .dependsOn(core)

lazy val doobie = project.from("doobie")
  .setName("useless-doobie")
  .setDescription("Doobie integration for useless")
  .setInitialImport("useless.doobie._")
  .configureModule
  .publish
  .dependsOn(core)

lazy val playJson = project.from("play-json")
  .setName("useless-play-json")
  .setDescription("Play JSON integration for useless")
  .setInitialImport("useless.playjson._")
  .configureModule
  .publish
  .dependsOn(core)

lazy val scalaz = project.from("scalaz")
  .setName("useless-scalaz")
  .setDescription("Scalaz integration for useless")
  .setInitialImport("useless.scalaz._")
  .configureModule
  .publish
  .dependsOn(core)

lazy val slick = project.from("slick")
  .setName("useless-slick")
  .setDescription("Slick integration for useless")
  .setInitialImport("useless.slick._")
  .configureModule
  .publish
  .dependsOn(core)

lazy val example = (project in file("example"))
  .setName("useless-example")
  .setDescription("useless example")
  .setInitialImport("useless.example._")
  .configureModule
  .noPublish
  .settings(mainClass := Some("useless.example.Example"))
  .dependsOn(core, cats, circe, doobie, playJson, scalaz, slick)

addCommandAlias("fullTest", ";test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")
addCommandAlias("relock", ";unlock;reload;update;lock")
