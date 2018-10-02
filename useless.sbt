import sbt._
import Settings._

lazy val root = project.root
  .setName("useless")
  .setDescription("Build of a simple process manager library")
  .configureRoot
  .aggregate(core)

lazy val core = project.from("core")
  .setName("core")
  .setDescription("Simple process manager library")
  .setInitialImport("_")
  .configureModule
  .configureTests()
  .settings(Compile / resourceGenerators += task[Seq[File]] {
    val file = (Compile / resourceManaged).value / "useless-version.conf"
    IO.write(file, s"version=${version.value}")
    Seq(file)
  })

addCommandAlias("fullTest", ";test;scalastyle")

addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")

addCommandAlias("relock", ";unlock;reload;update;lock")
