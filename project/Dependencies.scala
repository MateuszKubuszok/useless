import sbt._

import Dependencies._

object Dependencies {

  // scala version
  val scalaOrganization = "org.scala-lang" // "org.typelevel"
  val scalaVersion      = "2.12.7" // "2.12.4-bin-typelevel-4"

  // build tools version
  val scalaFmtVersion = "1.5.1"

  // aspectj version
  val aspectjVersion = "1.9.1"

  // libraries versions
  val catsVersion     = "1.2.0"
  val monixVersion    = "3.0.0-RC1"
  val specs2Version   = "4.3.3"

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  // functional libraries
  val cats               = "org.typelevel"                %% "cats-core"                 % catsVersion
  // async
  val monixExecution     = "io.monix"                     %% "monix-execution"           % monixVersion
  val monixEval          = "io.monix"                     %% "monix-eval"                % monixVersion
  // testing
  val spec2Core          = "org.specs2"                   %% "specs2-core"               % specs2Version
  val spec2Scalacheck    = "org.specs2"                   %% "specs2-scalacheck"         % specs2Version
}

trait Dependencies {

  val scalaOrganizationUsed = scalaOrganization
  val scalaVersionUsed = scalaVersion

  val scalaFmtVersionUsed = scalaFmtVersion

  val aspectjVersionUsed = aspectjVersion

  // resolvers
  val commonResolvers = resolvers

  val mainDeps = Seq()

  val testDeps = Seq(spec2Core, spec2Scalacheck)

  implicit class ProjectRoot(project: Project) {

    def root: Project = project in file(".")
  }

  implicit class ProjectFrom(project: Project) {

    private val commonDir = "modules"

    def from(dir: String): Project = project in file(s"$commonDir/$dir")
  }

  implicit class DependsOnProject(project: Project) {

    private val testConfigurations = Set("test", "fun", "it")
    private def findCompileAndTestConfigs(p: Project) =
      (p.configurations.map(_.name).toSet intersect testConfigurations) + "compile"

    private val thisProjectsConfigs = findCompileAndTestConfigs(project)
    private def generateDepsForProject(p: Project) =
      p % (thisProjectsConfigs intersect findCompileAndTestConfigs(p) map (c => s"$c->$c") mkString ";")

    def compileAndTestDependsOn(projects: Project*): Project =
      project dependsOn (projects.map(generateDepsForProject): _*)
  }
}
