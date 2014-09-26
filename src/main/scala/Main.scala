package com.todesking.jcon

import org.rogach.scallop.ScallopConf
import java.sql.Connection

import Implicits._

object Main {
  class Args(raw:Array[String]) extends ScallopConf(raw) {
    val user = opt[String]()
    val password = opt[String]()
    val url = trailArg[String]()
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
    if(!args.url.supplied) {
      args.printHelp()
    } else {
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
      Some(java.sql.DriverManager.getConnection(args.url(), props))
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

