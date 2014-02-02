import java.nio.file.Paths

name := "gpioControl"

version := "0.1.1-SNAPSHOT"

commands ++= Seq(PiDev.runJavah, PiDev.printState, PiDev.copyToPi)

mainClass := Some("org.pipifax.gpio.GpioController")

PiDev.piMountDirectory := Paths.get("/media/pipifax")

PiDev.piMountCommand := Seq("/home/andi/pipifax")

PiDev.piUmountCommand := Seq("fusermount", "-u", PiDev.piMountDirectory.value.toString)

PiDev.piTargetDirectory := Paths.get("gpioControl")

PiDev.nativeCode := Paths.get("native")

PiDev.compileCommand := "gcc -fPIC -c org_pipifax_gpio_GpioNative.cpp -I /usr/lib/jvm/java-7-openjdk-armhf/include"

PiDev.linkCommand := "gcc org_pipifax_gpio_GpioNative.o -shared -o gpionative.so -WL,-soname,gpionative"

PiDev.piMainArgs := "native/gpionative.so"

PiDev.isPiMounted

PiDev.mountPi

PiDev.umountPi

PiDev.umountPiTask

PiDev.piJarsToCopy

PiDev.piAbsoluteTargetDirectory

