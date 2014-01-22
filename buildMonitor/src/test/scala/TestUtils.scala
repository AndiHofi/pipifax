package org.pipifax.monitor

import org.scalatest.{Spec, Matchers}
import Matchers._


class TestUtils extends Spec {

  object `using function` {
    def `should close resource` {
      val closable = new TestClosable {}
      var calledF = false
      val result = new Object()

      utils.using(closable) {
        c =>
          c shouldBe closable
          calledF = true
          result
      } shouldBe result

      calledF shouldBe true
      closable.wasClosed shouldBe true
    }

    def `should throw callee exception` {
      val ex = new Exception()
      val closable = new TestClosable {}
      var calledF = false

      intercept[Exception] {
        utils.using(closable) {
          c =>
            c shouldBe closable
            calledF = true
            throw ex
        }
      } shouldBe ex

      calledF shouldBe true
      closable.wasClosed shouldBe true
    }

    def `should throw exception of close` {
      val ex = new Exception()
      val closable = new FailingTestClosable(ex)

      intercept[Exception] {
        utils.using(closable) {
          c =>
            c shouldBe closable

            "ignored result"
        }
      } shouldBe ex
    }

    def `should throw callee exception and add suppressed exception` {
      val calleeEx = new Exception()
      val closeEx = new Exception()
      val closable = new FailingTestClosable(closeEx)

      val caught = intercept[Exception] {
        utils.using(closable) {
          c =>
            c shouldBe closable

            throw calleeEx
        }
      }

      caught shouldBe calleeEx
      caught.getSuppressed should equal(Array(closeEx))
    }
  }


}

class FailingTestClosable(x: Throwable) extends TestClosable {
  override def close(): Unit = {
    super.close()
    throw x
  }
}

abstract class TestClosable extends Object with AutoCloseable {
  var wasClosed: Boolean = false;

  override def close(): Unit = {
    wasClosed = true
  }
}
