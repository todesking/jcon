package com.todesking.jcon

import org.rogach.scallop.ScallopConf
import java.sql.Connection

object Imports extends scalaz.syntax.ToIdOps
import Imports._

class Args(raw:Array[String]) extends ScallopConf(raw) {
  val user = opt[String]()
  val password = opt[String]()
  val url = trailArg[String]()
}


object Main {

  def using[A <: {def close():Unit}, B](resource:A)(f:A => B):B = {
    try { f(resource) } finally { resource.close() }
  }

  def main(raw:Array[String]):Unit = {
    val args = new Args(raw)
    if(!args.url.supplied) args.printHelp()
    else {
      println(s"connecting to url: ${args.url()}")
      val url = args.url()

      val props = new java.util.Properties()
      args.user.foreach(props.setProperty("user", _))
      args.password.foreach(props.setProperty("password", _))

      using(java.sql.DriverManager.getConnection(url, props)) {con =>
        val ctx = new Context(con, new Out(System.out))
        runREPL(ctx)
      }
    }
  }

  def runREPL(ctx:Context):Unit = {
    val break = readCommand().execute(ctx)
    if(break) return
    else runREPL(ctx)
  }

  def readCommand():Command = {
    print("> ")
    val line = readLine()
    Command.parse(line)
  }
}

case class Command(proc:Context=>Boolean) {
  def execute(ctx:Context):Boolean = proc(ctx)
}

class Context(val con:Connection, val out:Out)

class Out(val out:java.io.PrintStream) {
  def error(message:String):Unit = {
    out.println(s"ERROR: ${message}")
  }
  def result(message:String):Unit = {
    out.println(s"${message}")
  }
}

object Command {
  import scala.util.parsing.combinator._

  object Parser extends RegexParsers {
    val ALL = ("""exit|quit|:?q""".r ^^ {q => Quit}) | (":info" ^^ {_ => Info })
    def parse(content:String) = parseAll(ALL, content)
  }

  def parse(line:String):Command = {
    Parser.parse(line) getOrElse SyntaxError(line)
  }

  def SyntaxError(line:String) = Command {ctx => ctx.out.error(s"Syntax error: ${line}"); false}
  val Quit = Command {ctx => ctx.out.result("BYE"); true}
  val Info = Command {ctx =>
    ctx.out.result(s"CLIENT INFO: ${ctx.con.getClientInfo}")
    ctx.out.result(s"METADATA: ${ctx.con.getMetaData}")
    false
  }
}
