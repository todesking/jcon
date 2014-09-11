organization := "com.todesking"

name := "jcon"

version := "0.0.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.scala-lang" % "jline" % "2.11.0-M3",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "org.rogach" %% "scallop" % "0.9.5",
  "com.h2database" % "h2" % "1.4.181"
)
