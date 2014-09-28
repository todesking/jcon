package com.todesking.jcon

import Implicits._

case class Command(proc:Context=>Boolean) {
  def execute(ctx:Context):Boolean = proc(ctx)
}

object Command {
  import scala.util.parsing.combinator._

  object Parser extends RegexParsers {
    val ALL = ("""exit|quit|:?q""".r ^^ {q => Quit}) | (":i(nfo)?".r ^^ {_ => Info }) | (":h(elp)?|\\?".r ^^ {_ => Help} ) | ("[^:].*".r ^^ {q => Query(q)}) | ("" ^^ {_=> Empty})
    def parse(content:String) = parseAll(ALL, content)
  }

  def parse(line:String):Command = {
    Parser.parse(line) getOrElse SyntaxError(line)
  }

  def SyntaxError(line:String) = Command {ctx => ctx.out.error(s"Syntax error: ${line}"); false}
  val Empty = Command { _ => false }
  val Quit = Command {ctx => ctx.out.result("BYE"); true}
  val Info = Command {ctx =>
    val meta = ctx.con.getMetaData
    ctx.out.result(s"DRIVER: ${meta.getDriverName} ${meta.getDriverVersion} ${ctx.con.getClass.getName}")
    ctx.out.result(s"URL: ${meta.getURL}")
    false
  }
  val Help = Command {ctx =>
    ctx.out.message(
      """Commands:
      |  exit, quit, :q - Quit
      |  :i[nfo] - Show connection information
      |  :h[elp] - Show this message
      |  other string - Execute query. The query should end with ";" character. Can be multilined.""".stripMargin)
    false
  }
  def Query(q:String) = Command {ctx =>
    def isComplete(q:String):Boolean = ";$".r.findFirstIn(q).isEmpty.unary_!
    def readCompleteQuery(initial:String):String = {
      var query = initial
      while(!isComplete(query)) {
        query += "\n"
        query += ctx.in.readLine("  -> ")
      }
      query
    }
    val query = readCompleteQuery(q)
    val con = ctx.con
    try {
      for {
        st <- using(con.createStatement())
      } {
        st.execute(query)
        ctx.out.result(st)
      }
    } catch {
      case e:java.sql.SQLException => ctx.out.error(e)
      case e:Exception => ctx.out.error(e)
    }
    false
  }
}
