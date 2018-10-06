import sbt._

import Dependencies._

object Dependencies {

  // scala version
  val scalaOrganization  = "org.scala-lang" // "org.typelevel"
  val scalaVersion       = "2.12.7" // "2.12.4-bin-typelevel-4"
  val crossScalaVersions = Seq("2.11.12", "2.12.7", "2.13.0-M4")

  // build tools version
  val scalaFmtVersion = "1.5.1"

  // libraries versions
  val doobieVersion         = "0.6.0-M3"
  val slickVersion          = "3.2.3"
  val catsEffectVersion     = "1.0.0"
  val scalazIOEffectVersion = "2.10.1"
  val circeVersion          = "0.10.0"
  val playJsonVersion       = "2.6.10"
  val specs2Version         = "4.3.4"

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  // database
  val doobie             = "org.tpolecat"                 %% "doobie-core"               % doobieVersion
  val slick              = "com.typesafe.slick"           %% "slick"                     % slickVersion
  // functional libraries
  val catsEffect         = "org.typelevel"                %% "cats-effect"               % catsEffectVersion
  val scalazIOEffect     = "org.scalaz"                   %% "scalaz-ioeffect"           % scalazIOEffectVersion
  // serialization
  val circe              = "io.circe"                     %% "circe-core"                % circeVersion
  val circeParser        = "io.circe"                     %% "circe-parser"              % circeVersion
  val playJson           = "com.typesafe.play"            %% "play-json"                 % playJsonVersion
  // testing
  val spec2Core          = "org.specs2"                   %% "specs2-core"               % specs2Version
  val spec2Scalacheck    = "org.specs2"                   %% "specs2-scalacheck"         % specs2Version
}

trait Dependencies {

  val scalaOrganizationUsed = scalaOrganization
  val scalaVersionUsed = scalaVersion
  val crossScalaVersionsUsed = crossScalaVersions

  val scalaFmtVersionUsed = scalaFmtVersion

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
