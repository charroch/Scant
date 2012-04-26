package scalatest

import android.os.Bundle
import android.app.Instrumentation

trait ADBLogger {
  this: Instrumentation =>

  def info(s: => String) {
    send(0, s)
  }

  def debug(s: => String) {
    send(1, s)
  }

  def warn(s: => String) {
    send(2, s)
  }

  def error(s: => String) {
    send(3, s)
  }

  def send(i: Int, s: => String) {
    val bundle = new Bundle();
    bundle.putString(Instrumentation.REPORT_KEY_STREAMRESULT, s)
    sendStatus(i, bundle)
  }
}