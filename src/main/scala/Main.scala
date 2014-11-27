package com.todesking.jcon

import org.rogach.scallop.ScallopConf

import java.sql.Connection
import java.io.File

import Implicits._

case class Exit(val code: Int) extends xsbti.Exit
class Main extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) = {
    Exit(Main.run(config.arguments))
  }
}

object Main {
  class Args(raw:Array[String]) extends ScallopConf(raw) {
    val user = opt[String]()
    val password = opt[String]()
    val url = trailArg[String](required = false)

    val drivers = opt[Boolean]()

    val driverDir = opt[String]()
    val driverClasses = opt[String]()

    def createConfig():Config = {
      var conf:Config = DefaultConfig
      val args = this

      if(driverDir.supplied)
        conf = new NestedConfig(conf) { override val driverDir = new File(args.driverDir()) }

      if(driverClasses.supplied)
        conf = new NestedConfig(conf) {
          override val uninitializedDriverClasses = parent.uninitializedDriverClasses ++ args.driverClasses().split(",")
        }
      conf
    }
  }

  def main(raw: Array[String]):Unit = {
    run(raw)
  }

  def run(raw: Array[String]): Int = {
    val args = new Args(raw)
    if(args.drivers()) {
      val config = args.createConfig()
      DriverLoader.initialize(config)
      DriverLoader.drivers.foreach {driver =>
        println(s"  ${DriverProxy.unwrap(driver).getClass.getName}")
      }
      0
    } else if(!args.url.supplied) {
      args.printHelp()
      1
    } else {
      val config = args.createConfig()
      DriverLoader.initialize(config)

      println(s"connecting to url: ${args.url()}")

      val url = args.url()

      for {
        terminal <- using[jline.Terminal](jline.TerminalFactory.create(), _.restore())
        reader <- using[jline.console.ConsoleReader](createReader(terminal, config.historyFile), closeConsoleReader(_))
        out <- Some(new Out(System.out, terminal))
        con <- createConnection(args, out)
        con <- using(con)
      } {
        Signal.registerHandler("CONT"){()=> terminal.reset() }
        terminal.init()
        val ctx = new Context(con, out, reader)
        ctx.out.message("type :help or ? to show usage")
        runREPL(ctx)
      }
      0
    }
  }

  def closeConsoleReader(reader:jline.console.ConsoleReader):Unit = {
    reader.getHistory match {
      case h:jline.console.history.FileHistory => h.flush()
      case _ =>
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

  def createReader(terminal:jline.Terminal, historyFile:Option[File]) = {
    val reader = new jline.console.ConsoleReader(System.in, System.out, terminal)
    historyFile.foreach { h => reader.setHistory(new jline.console.history.FileHistory(h)) }
    reader
  }

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

