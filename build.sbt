ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

val circeV           = "0.13.0"
val circeDeps = Seq(
  "io.circe" %% "circe-core"                   % circeV,
  "io.circe" %% "circe-parser"                 % circeV,
  "io.circe" %% "circe-generic"                % circeV,
)

lazy val root = (project in file("."))
  .settings(
    name := "kafka-offset-in-postgres",
    scalacOptions += "-Ymacro-annotations",

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-logging" % "0.5.7",
      "dev.zio" %% "zio-kafka" % "0.14.0",

      "dev.zio" %% "zio-interop-cats" % "2.3.1.0",
      "org.tpolecat" %% "skunk-core" % "0.0.24"
    ) ++ circeDeps
  )


