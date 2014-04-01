package org.pipifax.gpio

import java.nio.file._
import org.pipifax.gpioControl.PwmPrograms


object GpioController extends App {
  val Leds = Seq(0 -> 'yellow, 1 -> 'blue, 2 -> 'red1, 3 -> 'green1, 4 -> 'red2, 5 -> 'green2)
  System.load(Paths.get(args(0)).toAbsolutePath.toString)

  private val native: GpioNative = new GpioNative()
  try {
    val blink = native.createProgram(PwmPrograms.defaultBlink())
    val blink2 = native.createProgram(PwmPrograms.defaultDoubleBlink())
    val wobble = native.createProgram(PwmPrograms.defaultSinusWobble())

    require(native.init(false), "Failed to initialize native library!")

    println(args.toList)

    args.drop(1).toList match {
      case "wobble" :: leds =>
        val le = (leds map (_.toInt)).toArray
        le foreach (led => native.enableOutput(led, true))
        native.setProgram(le, wobble)
        Thread.sleep(10000)
        le foreach (native.enableOutput(_, false))
      case "ledTest" :: Nil =>
        println("LedTest")
        Thread.sleep(10000)
      case "ledTest" :: count :: Nil =>
        println("LedTest " + count.toInt)
        for(_ <- 1 to count.toInt) ledTest()
      case List(led) =>
        val found = Leds find { _._2.name.equals(led) }
        println(s"Testing $found")
        found foreach {
          case (pin, _) =>
            native.enableOutput(pin, true)
            native.writeDigital(pin, true)
            Thread.sleep(1000)
            native.enableOutput(pin, false)
        }
    }

  } finally assert(native.close())
  println("Success")

  def ledTest(): Unit = {
    for((led, _) <- Leds) {
      native.enableOutput(led, true)
      native.writeDigital(led, true)
      Thread.sleep(200)
      native.writeDigital(led, false)
      native.enableOutput(led, false)
    }
  }
}
