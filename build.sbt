seq(conscriptSettings :_*)

organization := "com.todesking"

name := "jcon"

version := "0.0.1+"

scalaVersion := "2.11.4"

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

publishTo := Some(Resolver.file("com.todesking",file("./repo/"))(Patterns(true, Resolver.mavenStyleBasePattern)))

sourceGenerators in Compile <+= (sourceManaged in Compile, version) map { (dir, v) =>
  val file = dir / "Version.scala"
  IO.write(file, s"""package com.todesking.jcon
    |object Version {
    |  val string = "${v}"
    |}""".stripMargin)
  Seq(file)
}

compile <<= (compile in Compile) dependsOn Def.task {
    val content = s"""[app]
      |  version: ${version.value.replaceAll("\\+$", "")}
      |  org: ${organization.value}
      |  name: ${name.value}
      |  class: com.todesking.jcon.Main
      |[scala]
      |  version: ${scalaVersion.value}
      |[repositories]
      |  local
      |  scala-tools-releases
      |  maven-central
      |  todesking: http://todesking.github.io/mvn/""".stripMargin
    val dir = (sourceDirectory in Compile).value / "conscript" / "jcon"
    dir.mkdirs()
    val launchconfig = dir / "launchconfig"
    IO.write(launchconfig, content)
  }
