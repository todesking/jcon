package com.todesking.jcon

import Implicits._

class Out(val out:java.io.PrintStream, val terminal:scala.tools.jline.Terminal) {
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
      val subRows:Seq[Seq[String]] = row.map(_.split("\n").toSeq).quadrilateralize("").transpose
      val maxHeight = subRows.map(_.size).max
      subRows.zipWithIndex.foreach {case (row, i) =>
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

