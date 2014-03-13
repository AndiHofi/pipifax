package org.pipifax.gpio

import java.nio.file._


object GpioController extends App {
  val Leds = Seq(4 -> 'yellow, 17 -> 'blue, 18 -> 'red1, 27 -> 'green1, 22 -> 'red2, 25 -> 'green2)
  System.load(Paths.get(args(0)).toAbsolutePath.toString)

  private val native: GpioNative = new GpioNative()
  try {
    require(native.init(false), "Yeah, loading library worked!!!")

    println(args.toList)

    args.drop(1).toList match {
      case List("ledTest") =>
        println("LedTest")
        ledTest()
      case List("ledTest", count) =>
        println("LedTest " + count.toInt)
        for(_ <- 1 to count.toInt) ledTest()
      case List(led) =>
        val found = Leds find { _._2.name.equals(led) }
        println(s"Testing $found")
        found foreach {
          case (pin, _) =>
            native.gpioFsel(pin, GpioConstants.FSEL_OUTP)
            native.writeDigital(pin, true)
            Thread.sleep(1000)
            native.gpioFsel(pin, GpioConstants.FSEL_INPT)
        }
    }

  } finally assert(native.close())
  println("Success")

  def ledTest(): Unit = {
    for((led, _) <- Leds) {
      native.gpioFsel(led, GpioConstants.FSEL_OUTP)
      native.writeDigital(led, true)
      Thread.sleep(200)
      native.writeDigital(led, false)
      native.gpioFsel(led, GpioConstants.FSEL_INPT)
    }
  }


  //    gpio export 17 out
  //        gpio -g write 17 1
  //    gpio export 18 out
  //        gpio -g write 18 1
  //    gpio -g write 17 0
  //    gpio export 27 out
  //        gpio -g write 27 1
  //    gpio -g write 18 0
  //    gpio export 22 out
  //        gpio -g write 22 1
  //    gpio -g write 27 0
  //    gpio export 25 out
  //        gpio -g write 25 1
  //    gpio -g write 22 0
  //    gpio export 4 out
  //        gpio -g write 4 1
  //    gpio -g write 25 0
  //    gpio -g write 4 0
  //    gpio reset
}
