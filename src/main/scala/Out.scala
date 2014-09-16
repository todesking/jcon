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
    val raw = cols.map{case (i1, name) => Math.max(name.displayWidth, rows.map{r => r(i1 - 1).displayWidth}.++(Seq(0)).max)}
    optimize(raw, terminalWidth - (2 * cols.size)/*padding*/ - (cols.size - 1)/*sep*/ - 4/*start/end*/)
  }

  def optimize(ws:Seq[Int], max:Int) = {
    if(ws.sum <= max) ws
    else {
      val base = max / ws.size
      val ws2 = ws.map {w => (w, Math.min(w, base))}
      val rest = max - ws2.map{case (p, r) => r}.sum
      val div = Math.max(ws2.map{case (p,r) => p - r}.sum, 1)
      println(max)
      ws2.map{case (prefer, real) => real + rest * (prefer - real) / div} <| (println(_))
    }
  }

  def updateResult(count:Int):Unit = {
    result(s"${count} rows changed")
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

