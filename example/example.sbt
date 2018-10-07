libraryDependencies += "io.circe" %% "circe-generic" % Dependencies.circeVersion
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % Dependencies.slickVersion
libraryDependencies += "org.tpolecat" %% "doobie-postgres" % Dependencies.doobieVersion
libraryDependencies += "com.github.tminglei" %% "slick-pg" % "0.16.3"
libraryDependencies += "org.flywaydb" % "flyway-core" % "5.2.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
