package com.todesking.jcon
import java.io.File

abstract class Config {
  def userDir:File
  def driverDir:File
  def uninitializedDriverClasses:Set[String]
  def historyFile:Option[File]

  def driverJars:Array[File] =
    Option(driverDir.listFiles()) getOrElse Array() filter {f => f.getName.endsWith(".jar") && f.isFile() }
}

object DefaultConfig extends Config {
  override val userDir:File = new File(System.getProperty("user.home"), ".jcon")
  override val driverDir:File = new File(userDir, "drivers")
  override val uninitializedDriverClasses:Set[String] = Set("org.sqlite.JDBC")
  override val historyFile:Option[File] = None
}

class NestedConfig(val parent:Config) extends Config {
  override def userDir:File = parent.userDir
  override def driverDir:File = parent.driverDir
  override def uninitializedDriverClasses:Set[String] = parent.uninitializedDriverClasses
  override def historyFile:Option[File] = parent.historyFile
}
