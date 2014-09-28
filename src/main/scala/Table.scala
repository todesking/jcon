package com.todesking.jcon

object Table {
  def builder() = new Builder()
  class Builder {
    case class Column(num:Int, content:String)
    case class Row(cols:Seq[Column]) {
      def size:Int = cols.size
      def apply(i:Int):Column = cols(i)
    }
    object Row {
      def fromPlain(cols:Seq[String]):Row = {
        Row(
          cols.zipWithIndex.map { case (c, i) => Column(i, c) }
        )
      }
    }
    var calcDisplayWidth:(String => Int) = { _.size }
    private var header:Row = null
    private var rows:Seq[Row] = Seq()
    def columnSize:Int = header.size
    def rowSize:Int = rows.size

    def setHeader(header:Seq[String]):this.type = {
      this.header = Row.fromPlain(header)
      this
    }

    def addRow(cols:Seq[String]):this.type = {
      this.rows ++= Seq(Row.fromPlain(cols))
      this
    }

    def calcWidths(maxWidth:Int):Seq[Int] = {
      val raw = header.cols.map { case Column(i, name) =>
          Math.max(calcDisplayWidth(name), rows.map{ r => calcDisplayWidth(r(i).content) }.++(Seq(0)).max)
      }
      optimizeWidth(raw, maxWidth - (2 * columnSize)/*padding*/ - (columnSize - 1)/*sep*/ - 4/*start/end*/)
    }

    def optimizeWidth(ws:Seq[Int], max:Int) = {
      if(ws.sum <= max) ws
      else {
        val base = max / ws.size
        val ws2 = ws.map {w => (w, Math.min(w, base))}
        val rest = max - ws2.map{case (p, r) => r}.sum
        val div = Math.max(ws2.map{case (p,r) => p - r}.sum, 1)
        ws2.map { case (prefer, real) => real + rest * (prefer - real) / div}
      }
    }

    def quadrilateralize[A](self:Seq[Seq[A]], default:A):Seq[Seq[A]] = {
      val w = self.toIterator.map(_.size).max
      self.map {row =>
        for(c <- 0 until w) yield {
          if(c < row.size) row(c) else default
        }
      }
    }

    def pad(self:String, widthFunc:(String=>Int), maxWidth:Int, padding:Char, omit:String):String = {
      val w = widthFunc(self)
      if(w == maxWidth) self
      else if(w < maxWidth) self + padding.toString * (maxWidth - w)
      else shorten(self, widthFunc, maxWidth - widthFunc(omit)) + omit
    }

    def shorten(self:String, widthFunc:(String=>Int), width:Int):String = {
      shorten0(self, widthFunc, 0, width)
    }

    def shorten0(self:String, widthFunc:(String=>Int), index:Int, widthLeft:Int):String = {
      if(index >= self.size) return self

      val w = widthFunc(self.charAt(index).toString)
      if(w > widthLeft) self.substring(0, index) + (" " * widthLeft)
      else shorten0(self, widthFunc, index + 1, widthLeft - w)
    }

    def render(maxWidth:Int)(println:String=>Unit):Unit = {
      var widths = calcWidths(maxWidth)
      def rowsep() = println("+" + widths.map{w => "-" * (w + 2)}.mkString("+") + "+")
      def outRow(row:Row) = {
        val subRows:Seq[Seq[String]] = quadrilateralize(row.cols.map(_.content.split("\n").toSeq), "").transpose
        val maxHeight = subRows.map(_.size).max
        subRows.zipWithIndex.foreach {case (row, i) =>
          val sep = if(i == 0) "|" else ":"
          println(s"$sep " + row.zipWithIndex.map{case (r, i) => pad(r, calcDisplayWidth(_), widths(i), ' ', "...")}.mkString(s" $sep ") + " |")
        }
      }
      rowsep()
      outRow(header)
      rowsep()
      rows.foreach(outRow(_))
      rowsep()
    }
  }
}
