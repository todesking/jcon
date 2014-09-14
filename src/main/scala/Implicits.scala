package com.todesking.jcon

object Implicits extends scalaz.syntax.ToIdOps {
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

  implicit class SeqOfSeq[A](self:Seq[Seq[A]]) {
    def quadrilateralize(default:A):Seq[Seq[A]] = {
      val w = self.toIterator.map(_.size).max
      self.map {row =>
        for(c <- 0 until w) yield {
          if(c < row.size) row(c) else default
        }
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
