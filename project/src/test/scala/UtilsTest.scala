package org.pipifax.pidev

import org.scalatest.{Matchers, Spec}
import Matchers._
import org.pipifax.Utils
import Utils._

class UtilsTest extends Spec {
  object `MyRichString should` {

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

  object `MyRichPath should` {
    def `create Unix path string for single element path` {
      "path".nioPath.toUnixPath shouldEqual "path"
      ".".nioPath.toUnixPath shouldEqual "."
      "/".nioPath.toUnixPath shouldEqual "/"
    }

    def `create Unix path string for multi element path` {
      ("path".nioPath / "sub").toUnixPath shouldEqual "path/sub"
      toUnixPath("/".nioPath / "path" / "file.txt") shouldEqual "/path/file.txt"
      toUnixPath("path".nioPath / "sub" / "sub2" / "file.txt") shouldEqual "path/sub/sub2/file.txt"
    }

    def `should remove redundant . and ..` {
      toUnixPath("redundant/../".nioPath) shouldEqual "."
      toUnixPath("path/./././..".nioPath) shouldEqual "."
      toUnixPath("path/ignored/..".nioPath) shouldEqual "path"
    }
  }
}