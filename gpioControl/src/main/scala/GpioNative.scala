package org.pipifax.gpio

class GpioNative {
  @native def initPinMap(pins: Array[Byte]): Unit

  @native def init(debug: Boolean): Boolean

  @native def enableOutput(led: Int, enabled: Boolean): Unit

  @native def writeDigital(led: Int, value: Boolean): Unit

  @native def createProgram(onOffIntervals: Array[Int]): Int

  @native def setProgram(leds: Array[Int], program: Int): Unit

  @native def close(): Boolean
}
