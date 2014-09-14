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

      val props = new java.util.Properties()
      args.user.foreach(props.setProperty("user", _))
      args.password.foreach(props.setProperty("password", _))

      for {
        con <- using(java.sql.DriverManager.getConnection(url, props))
        terminal <- using[scala.tools.jline.Terminal](scala.tools.jline.TerminalFactory.create(), _.restore())
      } {
        registerSignal("CONT"){()=> terminal.reset() }
        terminal.init()
        val ctx = new Context(con, new Out(System.out, terminal), createReader(terminal))
        runREPL(ctx)
      }
    }
  }

  def createReader(terminal:scala.tools.jline.Terminal) =
    new scala.tools.jline.console.ConsoleReader(System.in, new java.io.PrintWriter(System.out), terminal)

  def runREPL(ctx:Context):Unit = {
    val quit = readCommand(ctx.in).execute(ctx)
    if(quit) return
    else runREPL(ctx)
  }

  def readCommand(reader:scala.tools.jline.console.ConsoleReader):Command = {
    val line = reader.readLine("> ")
    if(line == null) return Command.Quit
    else Command.parse(line)
  }
}

