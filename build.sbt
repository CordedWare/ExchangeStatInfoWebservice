ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "ExchangeStatInfoWebservice"
  )

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.1",
  "com.typesafe.akka" %% "akka-stream" % "2.6.17",
  "com.typesafe.akka" %% "akka-actor" % "2.6.17",
  "com.typesafe.akka" %% "akka-http" % "10.2.5",
  "com.typesafe.slick" %% "slick" % "3.5.0-M4",
  "org.postgresql" % "postgresql" % "42.3.0",
  "org.scala-lang.modules" %% "scala-xml" % "1.3.0"
)