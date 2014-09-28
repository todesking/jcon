package com.todesking.jcon

object Implicits extends scalaz.syntax.ToIdOps {
  private type HasClose = {def close():Unit}
  class Using[A](resource:A, close:A => Unit) {
    def apply[B](f:A => B):B =
      try { f(resource) } finally { close(resource) }
    def foreach(f:A => Unit) = apply(f)
  }

  def using[A <: HasClose](resource:A):Using[A] = {
    import scala.language.reflectiveCalls
    using(resource, _.close())
  }
  def using[A](resource:A, close:A => Unit) =
    new Using(resource, close)

  implicit class StringExt(self:String) {
    def displayWidth0:Int = {
      val halfs = """[\u0000-\u007f]""".r.findAllIn(self).size
      val fulls = self.size - halfs
      halfs + fulls * 2
    }

    def displayWidth:Int = {
      self.split("\n").map(_.displayWidth0).max
    }
    def consoleSafe():String =
      "[\u0000-\u0019]".r.replaceAllIn(self, (m:scala.util.matching.Regex.Match) => Character.toChars(m.matched.charAt(0) + 0x2400)(0).toString)
  }
}
