package com.todesking.jcon

import org.rogach.scallop.ScallopConf
import java.sql.Connection

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

    // initialize driver classes not supported JDBC4's service discovery mechanism
    config.uninitializedDriverClasses.foreach { klass =>
      register(Class.forName(klass, true, driverClassLoader).newInstance.asInstanceOf[Driver])
    }

    // initialize driver classes via service loader
    val serviceLoader = java.util.ServiceLoader.load(classOf[java.sql.Driver], driverClassLoader)

    val loadedDrivers:Set[Class[_]] = DriverManager.getDrivers.asScala.map(_.getClass).toSet
    serviceLoader.iterator.asScala.filter{ driver => !loadedDrivers.contains(driver.getClass) }.foreach { driver => register(driver) }
  }
}

class Config {
  import java.io.File
  lazy val userDir:File = new File(System.getProperty("user.home"), ".jcon")
  lazy val driverDir:File = new File(userDir, "drivers")
  lazy val defaultUninitializedDriverClasses:Set[String] = Set("org.sqlite.JDBC")
  lazy val uninitializedDriverClasses:Set[String] = defaultUninitializedDriverClasses

  def driverJars:Array[File] =
    Option(driverDir.listFiles()) getOrElse Array() filter {f => f.getName.endsWith(".jar") && f.isFile() }
}

object Main {
  class Args(raw:Array[String]) extends ScallopConf(raw) {
    val user = opt[String]()
    val password = opt[String]()
    val url = trailArg[String](required = false)

    val drivers = opt[Boolean]()
  }

  def registerSignal(sig:String)(handler:() =>Unit):Unit = {
    import sun.misc.{Signal, SignalHandler}
    Signal.handle(new Signal(sig), new SignalHandler() {
      override def handle(signal:Signal):Unit = {
        handler()
      }
    })
  }

  def main(raw:Array[String]):Unit = {
    val args = new Args(raw)
    if(args.drivers()) {
      val config = new Config()
      DriverLoader.initialize(config)
      DriverLoader.drivers.foreach {driver =>
        println(s"  ${DriverProxy.unwrap(driver).getClass.getName}")
      }
    } else if(!args.url.supplied) {
      args.printHelp()
    } else {
      val config = new Config()
      DriverLoader.initialize(config)

      println(s"connecting to url: ${args.url()}")

      val url = args.url()

      for {
        terminal <- using[jline.Terminal](jline.TerminalFactory.create(), _.restore())
        out <- Some(new Out(System.out, terminal))
        con <- createConnection(args, out)
        con <- using(con)
      } {
        registerSignal("CONT"){()=> terminal.reset() }
        terminal.init()
        val ctx = new Context(con, out, createReader(terminal))
        ctx.out.message("type :help or ? to show usage")
        runREPL(ctx)
      }
    }
  }

  def createConnection(args:Args, out:Out):Option[java.sql.Connection] = {
    val props = new java.util.Properties()
    args.user.foreach(props.setProperty("user", _))
    args.password.foreach(props.setProperty("password", _))

    try {
      Some(DriverLoader.getConnection(args.url(), props))
    } catch {
      case e:java.sql.SQLException =>
        out.error(s"Unable to connect: ${args.url()}. ${e.getMessage}")
        None
    }
  }

  def createReader(terminal:jline.Terminal) =
    new jline.console.ConsoleReader(System.in, System.out, terminal)

  def runREPL(ctx:Context):Unit = {
    val quit = readCommand(ctx.in).execute(ctx)
    if(quit) return
    else runREPL(ctx)
  }

  def readCommand(reader:jline.console.ConsoleReader):Command = {
    val line = reader.readLine("JDBC> ")
    if(line == null) return Command.Quit
    else Command.parse(line)
  }
}

