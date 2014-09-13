package com.todesking.jcon

import org.rogach.scallop.ScallopConf
import java.sql.Connection

object Imports extends scalaz.syntax.ToIdOps {
  private type HasClose = {def close():Unit}
  class Using[A](resource:A, close:A => Unit) {
    def apply[B](f:A => B):B =
      try { f(resource) } finally { close(resource) }
    def foreach(f:A => Unit) = apply(f)
  }

  def using[A <: HasClose](resource:A):Using[A] =
    using(resource, _.close())
  def using[A](resource:A, close:A => Unit) =
    new Using(resource, close)

  implicit class Transpose[A](self:Seq[Seq[A]]) {
    def transposeWithDefault(default:A):Seq[Seq[A]] = {
      val h = self.size
      val w = self.map(_.size).max
      for {
        c <- 0 until w
      } yield for {
        r <- 0 until h
      } yield {
        val row = self(r)
        if(c < row.size) row(c) else default
      }
    }
  }

  implicit class StringExt(self:String) {
    def displayWidth0:Int = {
      val halfs = """[\u0000-\u007f]""".r.findAllIn(self).size
      val fulls = self.size - halfs
      halfs + fulls * 2
    }

    def displayWidth:Int = {
      self.split("\n").map(_.displayWidth0).max
    }

    def pad(width:Int, padding:Char, omit:String):String = {
      val w = self.displayWidth0
      if(w == width) self
      else if(w < width) self + padding.toString * (width - w)
      else self.shortenByDisplayWidth(width - omit.displayWidth) + omit
    }

    def shortenByDisplayWidth(width:Int):String = {
      shortenByDisplayWidth0(0, width)
    }
    def shortenByDisplayWidth0(index:Int, widthLeft:Int):String = {
      if(index >= self.size) return self

      val w = self.charAt(index).toString.displayWidth0
      if(w > widthLeft) self.substring(0, index) + (" " * widthLeft)
      else shortenByDisplayWidth0(index + 1, widthLeft - w)
    }
    def consoleSafe():String =
      "[\u0000-\u0019]".r.replaceAllIn(self, (m:scala.util.matching.Regex.Match) => Character.toChars(m.matched.charAt(0) + 0x2400)(0).toString)
  }
}
import Imports._

object Main {
  class Args(raw:Array[String]) extends ScallopConf(raw) {
    val user = opt[String]()
    val password = opt[String]()
    val url = trailArg[String]()
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
        terminal.init()
        val ctx = new Context(con, new Out(System.out, terminal), createReader(terminal))
        runREPL(ctx)
      }
    }
  }

  def createReader(terminal:scala.tools.jline.Terminal) =
    new scala.tools.jline.console.ConsoleReader(System.in, new java.io.PrintWriter(System.out), terminal)

  def runREPL(ctx:Context):Unit = {
    val break = readCommand(ctx.in).execute(ctx)
    if(break) return
    else runREPL(ctx)
  }

  def readCommand(reader:scala.tools.jline.console.ConsoleReader):Command = {
    val line = reader.readLine("> ")
    if(line == null) return Command.Quit
    else Command.parse(line)
  }
}

case class Command(proc:Context=>Boolean) {
  def execute(ctx:Context):Boolean = proc(ctx)
}

class Context(val con:Connection, val out:Out, val in:scala.tools.jline.console.ConsoleReader)

class Out(val out:java.io.PrintStream, val terminal:scala.tools.jline.Terminal) {
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
    out.println(s"${message.consoleSafe}")
  }

  def calcWidths(cols:Seq[(Int, String)], rows:Seq[Seq[String]], terminalWidth:Int):Seq[Int] = {
    val raw = cols.map{case (i1, name) => Math.max(name.displayWidth, rows.map{r => r(i1 - 1).displayWidth}.max)}
    optimize(raw, terminalWidth)
  }

  def optimize(ws:Seq[Int], max:Int) = {
    if(ws.sum <= max) ws
    else {
      val maxWidth = 30
      ws.map {w => Math.min(w, maxWidth)}
    }
  }

  def result(res:java.sql.ResultSet):Unit = {
    val meta = res.getMetaData
    val cols = for(i1 <- 1 to meta.getColumnCount) yield (i1, meta.getColumnName(i1))
    val rows = scala.collection.mutable.ArrayBuffer.empty[Seq[String]]
    while(res.next()) {
      rows += (for { (i1, _) <- cols } yield res.getString(i1))
    }
    val displayWidth = terminal.getWidth
    var widths = calcWidths(cols, rows, displayWidth)
    // if(widths.sum + (widths.size - 1) * 3/*sep*/ + 4/*start+end*/ > displayWidth)
    def rowsep() = result("+" + cols.map{case (i1, name) => "-" * (widths(i1 - 1) + 2)}.mkString("+") + "+")
    def outRow(row:Seq[String]) = {
      val cols:Seq[Seq[String]] = row.map(_.split("\n").toSeq).transposeWithDefault("")
      val maxHeight = cols.map(_.size).max
      cols.transposeWithDefault("").transpose.zipWithIndex.foreach {case (row, i) =>
        val sep = if(i == 0) "|" else ">"
        result(s"$sep " + row.zipWithIndex.map{case (r, i) => r.pad(widths(i), ' ', "...")}.mkString(s" $sep ") + " |")
      }
    }
    rowsep()
    outRow(cols.map(_._2))
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
