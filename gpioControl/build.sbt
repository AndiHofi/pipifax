
import java.nio.file.Paths
import org.pipifax.pidev.PiDev._



name := "gpioControl"

version := "0.1.1-SNAPSHOT"

mainClass := Some("org.pipifax.gpio.GpioController")

piDevSettings

piRootMountPath := "/home/pi"

piMountDirectory := Paths.get("/media/pipifax")

piMountCommand := Seq("sshfs", sshUser.value + "@" + sshHost.value + ":" + piRootMountPath.value, piMountDirectory.value.toString)

piUmountCommand := Seq("fusermount", "-u", piMountDirectory.value.toString)

piTargetDirectory := Paths.get("gpioControl")

nativeCode := Paths.get("native")

compileCommand := "g++ -Wall -pthread -fPIC -c org_pipifax_gpio_GpioNative.cpp -I /usr/lib/jvm/java-7-openjdk-armhf/include"

linkCommand := "gcc org_pipifax_gpio_GpioNative.o bcm2835.o -shared -o gpionative.so -WL,-soname,gpionative"

piMainArgs := "native/gpionative.so $*"

sshHost := "pipifax"

sshUser := "pi"

sshAuthFile := Some(Paths.get("/home/andi/.ssh/pi2_rsa"))

sshPiFingerprint := "01:b7:6f:c9:14:b7:7b:30:7f:5c:82:55:4f:cb:24:6b"

loggerConfigFile := baseDirectory.value.toPath.resolve("../project/src/main/resources/logback.xml")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"
