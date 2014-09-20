organization := "com.todesking"

name := "jcon"

version := "0.0.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.scala-lang" % "jline" % "2.11.0-M3",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "org.rogach" %% "scallop" % "0.9.5"
)

libraryDependencies += "com.h2database" % "h2" % "1.4.181"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.32"

resolvers += "linter" at "http://hairyfotr.github.io/linteRepo/releases"

addCompilerPlugin("com.foursquare.lint" %% "linter" % "0.1-SNAPSHOT")

org.scalastyle.sbt.ScalastylePlugin.Settings

scalacOptions ++= Seq("-deprecation", "-feature")
