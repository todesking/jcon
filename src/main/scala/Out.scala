package com.todesking.jcon

import Implicits._

class Out(val out:java.io.PrintStream, val terminal:jline.Terminal) {
  def message(m:String):Unit = out.println(m)
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

  def updateResult(count:Int):Unit = {
    result(s"${count} rows changed")
  }

  def result(st:java.sql.Statement):Unit = {
    while(result1(st)) st.getMoreResults()
  }

  def result1(st:java.sql.Statement):Boolean = {
    val count = st.getUpdateCount
    if(count != -1) {updateResult(count); true}
    else {
      // Some jdbc driver(sqlite) throws SQLException when result set is not available
      val rs = try { st.getResultSet } catch { case e:java.sql.SQLException => null }
      if(rs != null) { using(rs) {rs => result(rs)}; true }
      else false
    }
  }

  def result(res:java.sql.ResultSet):Unit = {
    val table = Table.builder()

    val meta = res.getMetaData

    table.setHeader(
      (1 to meta.getColumnCount).map(meta.getColumnName(_)) )

    while(res.next()) {
      table.addRow(
        (1 to meta.getColumnCount).map{ i => Option(res.getString(i)) getOrElse "NULL" } )
    }

    table.render(terminal.getWidth)(result(_))
    result(s"${table.rowSize} rows in set")
  }
}

