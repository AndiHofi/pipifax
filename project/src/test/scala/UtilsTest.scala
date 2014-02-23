package org.pipifax.pidev

import org.scalatest.{Matchers, Spec}
import Matchers._

class UtilsTest extends Spec {
  object `MyRichString should` {
    import org.pipifax.Utils._

    def `pad empty string` {

      "".padLeft(-1, 'a') shouldEqual ""
      "".padLeft(0, 'a') shouldEqual ""
      "".padLeft(5, 'a') shouldEqual "aaaaa"
    }

    def `return long enhough String` {
      "aaa".padLeft(1, 'b') shouldEqual "aaa"
      "aaa".padLeft(3, 'b') shouldEqual "aaa"
      "Hello World".padTo(11, 'b') shouldEqual "Hello World"
      val x = "mustStay"
      x.padLeft(3, 'c') should (be theSameInstanceAs  x)
    }

    def `pad string to minimum length` {
      "123".padLeft(4, 'b') shouldEqual "b123"
      "123".padLeft(6, 'b') shouldEqual "bbb123"
      "1".padLeft(4, 'b') shouldEqual "bbb1"
    }
  }
}