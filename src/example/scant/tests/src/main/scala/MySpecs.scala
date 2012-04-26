import android.test.InstrumentationTestCase
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec
import scalatest.ADBLogger

class MySpecs extends FunSpec with ShouldMatchers {
  describe("a spec") {
    it("should do something") {
    }
  }
}

class ApplicationSpecs extends InstrumentationTestCase with FunSpec with ShouldMatchers  {
  describe("an application spec") {
    it("should allow for classloading ") {

    }
  }
}