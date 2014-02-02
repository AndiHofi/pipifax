package org.pipifax.gpio

class GpioNative {
  @native def init(debug: Boolean): Int

  @native def init(): Int

  @native def writeDigital(pin: Int, value: Boolean): Unit

  @native def close(): Int

  @native def gpioFsel(pin: Int, mode: Int): Unit

  @native def gpioWriteMulti(pinMask: Int, states: Int): Unit
}
