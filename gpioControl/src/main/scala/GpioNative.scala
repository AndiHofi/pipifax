package org.pipifax.gpio

class GpioNative {
  @native def init(debug: Boolean): Boolean

  @native def init(): Boolean

  @native def writeDigital(pin: Int, value: Boolean): Unit

  /**
   * Resolution of a blink-cycle of a pin in PWM mode.
   *
   * setPWMParams(5, 10) would mean that PWM leds blink once in 50 milliseconds with 10 different on and of times. 20 HZ pwm mode.

   *
   * @param jiffie resolution of the PWM timer in milliseconds
   * @param period number of jiffies in a single blink cycle
   */
  @native def setPWMParams(jiffie: Int, period: Int): Unit

  /**
   *
   * @param pin Pin to write to.
   * @param value Intensity between 0 and 1
   */
  @native def writeAnalog(pin: Int, value: Float)

  /**
   * Set pin in blinking mode.
   *
   * All led in blinking mode with the same rate are set to on at the same time. Not meant for delays smaller than 100 ms.
   *
   * @param pin Pin to set in blink mode.
   * @param delay Delay in milliseconds. Must be a multiple of 100
   */
  @native def writeBlink(pin: Int, delay: Int)

  /**
   * Smoothly change between on and of state following a sinus curve.
   *
   * @param pin Pin to set in wobble mode.
   */
  @native def writeWobble(pin: Int)

  @native def close(): Boolean

  @native def gpioFsel(pin: Int, mode: Int): Unit

  @native def gpioWriteMulti(pinMask: Int, states: Int): Unit

  @native def initPinMap(pins: Array[Byte]): Unit
}
