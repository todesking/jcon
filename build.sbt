import com.typesafe.sbt.SbtNativePackager._

import NativePackagerKeys._

organization := "com.todesking"

name := "jcon"

version := "0.0.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "jline" % "jline" % "2.11",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "org.rogach" %% "scallop" % "0.9.5"
)

/** JDBC drivers **/
libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.181",
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "mysql" % "mysql-connector-java" % "5.1.32",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
)

resolvers += "linter" at "http://hairyfotr.github.io/linteRepo/releases"

addCompilerPlugin("com.foursquare.lint" %% "linter" % "0.1-SNAPSHOT")

scalacOptions ++= Seq("-deprecation", "-feature")


packageArchetype.java_application

