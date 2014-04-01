package org.pipifax.gpio

import org.scalatest.{Matchers, Spec, BeforeAndAfter}
import Matchers._
import scala.math._
import org.pipifax.gpioControl.PwmPrograms

class GpioControlerTest extends Spec{
  object `Calculating pwm modes` {
    def `2 second wobble` {
      val wobble: Array[Int] = PwmPrograms.defaultSinusWobble()

      println(wobble.mkString(", "))

      wobble.length shouldBe (2 * 2000 / 16)
      wobble.min should (be >= 0 and be <= 5)
      wobble.max should (be >= 15 and be <= 16)

    }
  }

}
