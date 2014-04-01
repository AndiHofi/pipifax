/**
 * Created by andi on 31.03.14.
 */
package org.pipifax.gpioControl

import scala.math._

object PwmPrograms {
  val blinkOnce = {(duration: Int) =>
    Array(duration/2, duration / 2)
  }

  val defaultBlink = () => blinkOnce(2000)

  val doubleBlink = {(duration: Int, blinkLength: Int) =>
    Array((duration - blinkLength * 3), blinkLength, blinkLength, blinkLength)
  }

  val defaultDoubleBlink = () => doubleBlink(2000, 300)

  val sinusWobble = {(duration: Int, minValue: Float, maxValue: Float, resolution: Int) =>
    val states = duration / resolution

    val function = { (offset: Int) =>
      (sin(offset.toFloat * 2.0f * Pi.toFloat / states.toFloat).toFloat + 1f) * 0.5f * (maxValue - minValue) + minValue
    }

    val data = new Array[Int](2 * states)
    var offset = 0
    while (offset < states) {
      val brightNess = function(offset)
      val onTime = round(brightNess * resolution)
      val offTime = resolution - onTime

      data(offset * 2) = offTime
      data(offset * 2 + 1) = onTime

      offset += 1
    }

    data
  }

  val defaultSinusWobble = () => sinusWobble(2000, 0.1f, 1.0f, 16)
}
