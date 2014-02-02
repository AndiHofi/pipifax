package org.pipifax.gpio

import java.nio.file._


object GpioController extends App{
  System.load(Paths.get(args(0)).toAbsolutePath.toString)

  require(new GpioNative().init() == 0, "Yeah, loading library worked!!!")

  println("Success")

}
