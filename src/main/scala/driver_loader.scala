package com.todesking.jcon

import Implicits._
import scala.collection.JavaConverters._

/*
 https://docs.oracle.com/javase/7/docs/api/java/sql/DriverManager.html
 "When the method getConnection is called, the DriverManager will attempt to locate a suitable driver from amongst those loaded at initialization and those loaded explicitly using the same classloader as the current applet or application."

 For that reason, we need wrap some drivers.

 See http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
*/

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

    // initialize driver classes that not supported JDBC4's service discovery mechanism
    val unmanagedDrivers = config.uninitializedDriverClasses.map { klass =>
      Class.forName(klass, true, driverClassLoader).newInstance.asInstanceOf[Driver]
    }

    // Clear current registered drivers. Because they are not wrapped.
    deregisterAllDrivers()

    // Register unmanaged drivers
    unmanagedDrivers.foreach { driver => register(driver) }

    // Register drivers via ServiceLoader
    val serviceLoader = java.util.ServiceLoader.load(classOf[java.sql.Driver], driverClassLoader)
    serviceLoader.iterator.asScala.foreach { driver => register(driver) }
  }

  def deregisterAllDrivers():Unit = {
    drivers().foreach { d => DriverManager.deregisterDriver(d) }
  }
}

