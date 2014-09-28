package com.todesking.jcon

import Implicits._
import scala.collection.JavaConverters._

class DriverProxy(val original:java.sql.Driver) extends java.sql.Driver {
  def acceptsURL(x$1: String): Boolean = original.acceptsURL(x$1)
  def connect(x$1: String,x$2: java.util.Properties): java.sql.Connection = original.connect(x$1, x$2)
  def getMajorVersion(): Int = original.getMinorVersion()
  def getMinorVersion(): Int = original.getMinorVersion()
  def getParentLogger(): java.util.logging.Logger = original.getParentLogger()
  def getPropertyInfo(x$1: String,x$2: java.util.Properties): Array[java.sql.DriverPropertyInfo] = original.getPropertyInfo(x$1, x$2)
  def jdbcCompliant(): Boolean = original.jdbcCompliant()
}

object DriverProxy {
  import java.sql.Driver

  def wrapIfNeeded(driver:Driver, classloader:ClassLoader):Driver = driver match {
    case d if d.getClass != (try { Class.forName(d.getClass.getName, true, classloader) } catch { case e:ClassNotFoundException => null }) =>
      new DriverProxy(d)
    case d => d
  }
  def unwrap(driver:Driver):Driver = driver match {
    case d:DriverProxy => d.original
    case d => d
  }
}

object DriverLoader {
  import java.sql.{DriverManager, Driver, Connection}

  def getConnection(url:String, properties:java.util.Properties):Connection = {
    DriverManager.getConnection(url, properties)
  }

  def drivers():Seq[Driver] = DriverManager.getDrivers.asScala.toSeq

  def initialize(config:Config):Unit = {
    val systemClassLoader = getClass.getClassLoader
    val driverClassLoader = java.net.URLClassLoader.newInstance(config.driverJars.map(_.toURI.toURL).toArray, systemClassLoader)

    def register(driver:Driver) = DriverManager.registerDriver(DriverProxy.wrapIfNeeded(driver, systemClassLoader))

    // First, clear all auto loaded drivers
    deregisterAllDrivers()

    // initialize driver classes not supported JDBC4's service discovery mechanism
    config.uninitializedDriverClasses.foreach { klass =>
      register(Class.forName(klass, true, driverClassLoader).newInstance.asInstanceOf[Driver])
    }

    // initialize driver classes via service loader
    val serviceLoader = java.util.ServiceLoader.load(classOf[java.sql.Driver], driverClassLoader)

    serviceLoader.iterator.asScala.foreach { driver => register(driver) }
  }

  def deregisterAllDrivers():Unit = {
    drivers().foreach { d => DriverManager.deregisterDriver(d) }
  }
}

