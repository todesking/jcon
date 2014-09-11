package com.todesking.jcon

import org.rogach.scallop.ScallopConf
import java.sql.Connection

object Imports extends scalaz.syntax.ToIdOps {
  private type HasClose = {def close():Unit}
  class Using[A <: HasClose](resource:A) {
    def apply[B](f:A => B):B =
      try { f(resource) } finally { resource.close() }
    def foreach(f:A => Unit) = apply(f)
  }

  def using[A <: HasClose](resource:A):Using[A] =
    new Using(resource)
}
import Imports._

class Args(raw:Array[String]) extends ScallopConf(raw) {
  val user = opt[String]()
  val password = opt[String]()
  val url = trailArg[String]()
}


object Main {

  def main(raw:Array[String]):Unit = {
    val args = new Args(raw)
    if(!args.url.supplied) args.printHelp()
    else {
      println(s"connecting to url: ${args.url()}")
      val url = args.url()

      val props = new java.util.Properties()
      args.user.foreach(props.setProperty("user", _))
      args.password.foreach(props.setProperty("password", _))

      val terminal = scala.tools.jline.TerminalFactory.create()
      terminal.init()
      val reader = new scala.tools.jline.console.ConsoleReader(System.in, new java.io.PrintWriter(System.out), terminal)

      using(java.sql.DriverManager.getConnection(url, props)) {con =>
        val ctx = new Context(con, new Out(System.out), reader)
        runREPL(ctx)
      }
      terminal.restore()
    }
  }

  def runREPL(ctx:Context):Unit = {
    val break = readCommand(ctx.in).execute(ctx)
    if(break) return
    else runREPL(ctx)
  }

  def readCommand(reader:scala.tools.jline.console.ConsoleReader):Command = {
    val line = reader.readLine("> ")
    Command.parse(line)
  }
}

case class Command(proc:Context=>Boolean) {
  def execute(ctx:Context):Boolean = proc(ctx)
}

class Context(val con:Connection, val out:Out, val in:scala.tools.jline.console.ConsoleReader)

class Out(val out:java.io.PrintStream) {
  def error(e:Throwable):Unit = {
    error(e.toString)
    e.getStackTrace.foreach{st => error(st.toString)}
  }
  def error(e:java.sql.SQLException):Unit = {
    error(e.getMessage)
  }
  def error(message:String):Unit = {
    out.println(s"ERROR: ${message}")
  }
  def result(message:String):Unit = {
    out.println(s"${message}")
  }
  def result(res:java.sql.ResultSet):Unit = {
    val meta = res.getMetaData
    val cols = for(i1 <- 1 to meta.getColumnCount) yield (i1, meta.getColumnName(i1), meta.getColumnType(i1))
    val rows = scala.collection.mutable.ArrayBuffer.empty[Seq[String]]
    while(res.next()) {
      rows += (for { (i1, _, _) <- cols } yield res.getObject(i1).toString)
    }
    val widths = cols.map{case (i1, name, _) => Seq(name.size, rows.maxBy{row => row(i1 - 1).size}.size).max}
    def rowsep() = result("+" + cols.map{case (i1, name, _) => "-" * (widths(i1 - 1) + 2)}.mkString("+") + "+")
    def outRow(row:Seq[String]) =
      result("| " + row.zipWithIndex.map{case (r, i) => s"%-${widths(i)}s".format(r)}.mkString(" | ") + " |")
    rowsep()
    result("| " + cols.map{case (i1, name, _) => s"%-${widths(i1 - 1)}s".format(name)}.mkString(" | ") + " |")
    rowsep()
    rows.foreach(outRow(_))
    rowsep()
  }
}

object Command {
  import scala.util.parsing.combinator._

  object Parser extends RegexParsers {
    val ALL = ("""exit|quit|:?q""".r ^^ {q => Quit}) | (":info" ^^ {_ => Info }) | ("[^:].*".r ^^ {q => Query(q)})
    def parse(content:String) = parseAll(ALL, content)
  }

  def parse(line:String):Command = {
    Parser.parse(line) getOrElse SyntaxError(line)
  }

  def SyntaxError(line:String) = Command {ctx => ctx.out.error(s"Syntax error: ${line}"); false}
  val Quit = Command {ctx => ctx.out.result("BYE"); true}
  val Info = Command {ctx =>
    val meta = ctx.con.getMetaData
    ctx.out.result(s"DRIVER: ${meta.getDriverName} ${meta.getDriverVersion} ${ctx.con.getClass.getName}")
    ctx.out.result(s"METADATA: ${meta}")
    ctx.out.result(s"CLIENT INFO: ${ctx.con.getClientInfo}")
    false
  }
  def Query(q:String) = Command {ctx =>
    val con = ctx.con
    try {
      for {
        st <- using(con.createStatement())
        result <- using(st.executeQuery(q))
      } {
        ctx.out.result(result)
      }
    } catch {
      case e:java.sql.SQLException => ctx.out.error(e)
      case e:Exception => ctx.out.error(e)
    }
    false
  }
}
