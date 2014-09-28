package com.todesking.jcon

object Signal {
  def registerHandler(sig:String)(handler:() =>Unit):Unit = {
    import sun.misc.{Signal => JSignal, SignalHandler}
    JSignal.handle(new JSignal(sig), new SignalHandler() {
      override def handle(signal:JSignal):Unit = {
        handler()
      }
    })
  }
}
