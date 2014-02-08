
import java.nio.file.Paths
import org.pipifax.pidev.PiDev._



name := "gpioControl"

version := "0.1.1-SNAPSHOT"

mainClass := Some("org.pipifax.gpio.GpioController")

piDevSettings

piMountDirectory := Paths.get("/media/pipifax")

piMountCommand := Seq("/home/andi/pipifax")

piUmountCommand := Seq("fusermount", "-u", piMountDirectory.value.toString)

piTargetDirectory := Paths.get("gpioControl")

nativeCode := Paths.get("native")

compileCommand := "gcc -fPIC -c org_pipifax_gpio_GpioNative.cpp -I /usr/lib/jvm/java-7-openjdk-armhf/include"

linkCommand := "gcc org_pipifax_gpio_GpioNative.o -shared -o gpionative.so -WL,-soname,gpionative"

piMainArgs := "native/gpionative.so"

umountPiTask
