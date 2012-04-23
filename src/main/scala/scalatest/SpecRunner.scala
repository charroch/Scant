package org.scalatest.tools

import _root_.android.app.Activity
import _root_.android.app.Instrumentation
import _root_.android.app.Instrumentation._
import _root_.android.os.Bundle
import _root_.android.os.Looper
import _root_.android.test.AndroidTestCase
import _root_.android.test.InstrumentationTestCase
import _root_.dalvik.system.DexFile
import dalvik.system.DexFile
import java.io.File
import android.os.{Looper, Bundle}
import android.test.{AndroidTestCase, InstrumentationTestCase}
import scala.collection.JavaConversions._
import android.app.{Activity, Instrumentation}
import org.scalatest._
import android.content.Context
import android.util.Log


class SpecRunner extends SpecRunnerComponent with DefaultInstrumentationReporter

abstract class SpecRunnerComponent extends Instrumentation with InstrumentationReporter {

  override def onCreate(arguments: Bundle) {
    super.onCreate(arguments);
    Log.i("TEST", "Bundle received" + arguments)
    start()
  }

  override def onStart() {
    Looper.prepare()
    val dexFile = new DexFile(new File(getContext.getApplicationInfo.publicSourceDir));

    Log.i("TEST", "Bundle received" + dexFile.entries()
      .withFilter(isSpec)
      .collect(asSuite).collect(injectContext).foreach(println))


    dexFile.entries()
      .withFilter(isSpec)
      .collect(asSuite)
      .map(injectContext andThen injectInstrumentation)
      .foreach(run)



    finish(Activity.RESULT_OK, new Bundle())
  }

  override def onException(obj: Object, e: Throwable) = {
    super.onException(obj, e)
  }

  def run(s: Any) {
    s match {
      case m: Suite => m.run(None, this.reporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      case _ => {
        val mTestResult = new Bundle();
        mTestResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT, "Not a ScalaTest spec... ")
        this.sendStatus(0, mTestResult)
      }
    }
  }

  val asSuite: PartialFunction[String, Suite] = (klass: String) =>
    getContext.getClassLoader.loadClass(klass).newInstance() match {
      case s: Suite => s
    }

  val injectInstrumentation: PartialFunction[Suite, Suite] = (s: Suite) => s match {
    case i: InstrumentationTestCase => i.injectInsrumentation(this); i
    case _ => s
  }

  val injectContext: PartialFunction[Suite, Suite] = (s: Suite) => s match {
    case a: AndroidTestCase => a.setContext(getTargetContext); a
    case _ => s
  }

  val injectTestContext = (s: Suite) => s match {
    case a: {
      def setTestContext(c: Context): Unit
    } => a.setTestContext(getContext); a
    case _ => s
  }

  def isSpec(klass: String): Boolean = {
    klass.endsWith("Spec") || klass.endsWith("Specs")
  }
}


trait InstrumentationReporter {
  i: Instrumentation =>
  def reporter: Reporter
}

trait DefaultInstrumentationReporter extends InstrumentationReporter {
  i: Instrumentation =>
  def reporter = new SimpleInstrumentationReporter(i)
}

class SimpleInstrumentationReporter(inst: Instrumentation) extends StringReporter(false, false, false, false) {
  final val ansiReset = "\033[0m"

  protected def printPossiblyInColor(text: String, ansiColor: String) = {
    val mTestResult = new Bundle();
    mTestResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT, ansiColor + text + ansiReset + '\n')
    inst.sendStatus(0, mTestResult)
  }

  def dispose() {
  }
}