import sbt._
import Settings._

lazy val root = project.root
  .setName("useless")
  .setDescription("Build of a simple process manager library")
  .configureRoot
  .aggregate(common, first, second)

lazy val common = project.from("common")
  .setName("common")
  .setDescription("Common utilities")
  .setInitialImport("_")
  .configureModule
  .configureTests()
  .configureFunctionalTests()
  .configureIntegrationTests()
  .settings(Compile / resourceGenerators += task[Seq[File]] {
    val file = (Compile / resourceManaged).value / "useless-version.conf"
    IO.write(file, s"version=${version.value}")
    Seq(file)
  })

lazy val first = project.from("first")
  .setName("first")
  .setDescription("First project")
  .setInitialImport("first._")
  .configureModule
  .configureTests()
  .compileAndTestDependsOn(common)
  .configureRun("useless.first.First")

lazy val second = project.from("second")
  .setName("second")
  .setDescription("Second project")
  .setInitialImport("second._")
  .configureModule
  .configureTests()
  .compileAndTestDependsOn(common)
  .configureRun("useless.second.Second")

addCommandAlias("fullTest", ";test;fun:test;it:test;scalastyle")

addCommandAlias("fullCoverageTest", ";coverage;test;fun:test;it:test;coverageReport;coverageAggregate;scalastyle")

addCommandAlias("relock", ";unlock;reload;update;lock")
