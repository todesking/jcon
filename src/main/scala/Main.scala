package com.todesking.jcon

import org.rogach.scallop.ScallopConf
import java.sql.Connection

import Implicits._

object Main {
  class Args(raw:Array[String]) extends ScallopConf(raw) {
    val user = opt[String]()
    val password = opt[String]()
    val url = trailArg[String](required = false)

    val drivers = opt[Boolean]()

    val driverDir = opt[String]()
    val driverClasses = opt[String]()

    def createConfig():Config = {
      var conf = Config.default
      if(driverDir.supplied) conf = conf.copy(driverDir = new java.io.File(driverDir()))
      if(driverClasses.supplied) conf = conf.copy(uninitializedDriverClasses = conf.uninitializedDriverClasses ++ driverClasses().split(","))
      conf
    }
  }

  def main(raw:Array[String]):Unit = {
    val args = new Args(raw)
    if(args.drivers()) {
      val config = args.createConfig()
      DriverLoader.initialize(config)
      DriverLoader.drivers.foreach {driver =>
        println(s"  ${DriverProxy.unwrap(driver).getClass.getName}")
      }
    } else if(!args.url.supplied) {
      args.printHelp()
    } else {
      val config = args.createConfig()
      DriverLoader.initialize(config)

      println(s"connecting to url: ${args.url()}")

      val url = args.url()

      for {
        terminal <- using[jline.Terminal](jline.TerminalFactory.create(), _.restore())
        out <- Some(new Out(System.out, terminal))
        con <- createConnection(args, out)
        con <- using(con)
      } {
        Signal.registerHandler("CONT"){()=> terminal.reset() }
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
    val line = reader.readLine("\nJDBC> ")
    if(line == null) return Command.Quit
    else Command.parse(line)
  }
}

