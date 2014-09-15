package com.todesking.jcon

import Implicits._

case class Command(proc:Context=>Boolean) {
  def execute(ctx:Context):Boolean = proc(ctx)
}

object Command {
  import scala.util.parsing.combinator._

  object Parser extends RegexParsers {
    val ALL = ("""exit|quit|:?q""".r ^^ {q => Quit}) | (":info" ^^ {_ => Info }) | (":help|\\?".r ^^ {_ => Help} ) | ("[^:].*".r ^^ {q => Query(q)})
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
  val Help = Command {ctx =>
    ctx.out.message(
      """
      |Commands:
      |  exit, quit, :q - Quit
      |  :info - Show connection information
      |  :help - Show this message
      |  other string - Execute query. The query should end with ";" character. Can be multilined.
      """.stripMargin)
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
    def showResults(st:java.sql.Statement):Unit = {
      while(showResult(st)) st.getMoreResults()
    }
    def showResult(st:java.sql.Statement):Boolean = {
      val rs = st.getResultSet
      val count = st.getUpdateCount
      if(rs != null) { using(rs) {rs => ctx.out.result(rs)}; true }
      else if(count != -1) {ctx.out.updateResult(count); true}
      else false
    }
    val query = readCompleteQuery(q)
    val con = ctx.con
    try {
      for {
        st <- using(con.createStatement())
      } {
        st.execute(query)
        showResults(st)
      }
    } catch {
      case e:java.sql.SQLException => ctx.out.error(e)
      case e:Exception => ctx.out.error(e)
    }
    false
  }
}
