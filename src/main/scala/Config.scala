package com.todesking.jcon
import java.io.File

case class Config(
    val userDir:File,
    val driverDir:File,
    val uninitializedDriverClasses:Set[String]
){
  def driverJars:Array[File] =
    Option(driverDir.listFiles()) getOrElse Array() filter {f => f.getName.endsWith(".jar") && f.isFile() }
}

object Config {
  val default:Config = {
    val userDir:File = new File(System.getProperty("user.home"), ".jcon")
    val driverDir:File = new File(userDir, "drivers")
    val uninitializedDriverClasses:Set[String] = Set("org.sqlite.JDBC")

    Config(userDir, driverDir, uninitializedDriverClasses)
  }
}
