import sbt._
import Settings._

lazy val root = project.root
  .setName("useless")
  .setDescription("Build of a simple process manager library")
  .configureRoot
  .noPublish
  .aggregate(core, cats, circe, scalaz)

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

lazy val scalaz = project.from("scalaz")
  .setName("useless-scalaz")
  .setDescription("Scalaz integration for useless")
  .setInitialImport("useless.scalaz._")
  .configureModule
  .publish
  .dependsOn(core)

addCommandAlias("fullTest", ";test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")
addCommandAlias("relock", ";unlock;reload;update;lock")
