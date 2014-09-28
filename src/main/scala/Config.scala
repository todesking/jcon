package com.todesking.jcon

class Config {
  import java.io.File
  lazy val userDir:File = new File(System.getProperty("user.home"), ".jcon")
  lazy val driverDir:File = new File(userDir, "drivers")
  lazy val defaultUninitializedDriverClasses:Set[String] = Set("org.sqlite.JDBC")
  lazy val uninitializedDriverClasses:Set[String] = defaultUninitializedDriverClasses

  def driverJars:Array[File] =
    Option(driverDir.listFiles()) getOrElse Array() filter {f => f.getName.endsWith(".jar") && f.isFile() }
}
