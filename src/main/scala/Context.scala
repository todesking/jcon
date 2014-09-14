package com.todesking.jcon

import java.sql.Connection

class Context(val con:Connection, val out:Out, val in:scala.tools.jline.console.ConsoleReader)
