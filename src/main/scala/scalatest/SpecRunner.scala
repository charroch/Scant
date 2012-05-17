package org.scalatest.tools

import dalvik.system.DexFile
import java.io.File
import android.os.{Looper, Bundle}
import android.test.{AndroidTestCase, InstrumentationTestCase}
import scala.collection.JavaConversions._
import android.app.{Activity, Instrumentation}
import org.scalatest._
import android.content.Context
import scalatest.ADBLogger
import util.matching.Regex

class SpecRunner extends SpecRunnerComponent with DefaultInstrumentationReporter

abstract class SpecRunnerComponent extends Instrumentation with InstrumentationReporter with ADBLogger {

  var test: Option[Regex] = None

  override def onCreate(arguments: Bundle) {
    super.onCreate(arguments);
    debug("Starting running test\n")
    debug(arguments.getString("class"))
    test = InstrumentationOption(arguments).filter
    start()
  }

  override def onStart() {
    Looper.prepare()
    val dexFile = new DexFile(new File(getContext.getApplicationInfo.publicSourceDir));
    dexFile.entries()
      .withFilter(filterTest(test))
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

  def filterTest(reg: Option[Regex])(klass: String): Boolean = {
    val specs = reg.getOrElse(""".*Spec(s)?""".r)
    klass match {
      case specs(_) => true
      case _ => false
    }
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

case class InstrumentationOption(filter: Option[Regex])

object InstrumentationOption {
  def apply(bundle: Bundle) = new InstrumentationOption(
    if (bundle.containsKey("class")) {

      Some(bundle.getString("class").r)
    } else {
      None
    }
  )
}
