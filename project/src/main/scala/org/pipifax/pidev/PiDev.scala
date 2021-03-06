package org.pipifax.pidev

import sbt._
import sbt.Inc
import sbt.Keys._
import sbt.Task
import sbt.Value
import scala.Some
import scala.util.{Try, Success, Failure}
import scala.collection.JavaConverters._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.nio.charset.Charset
import complete.DefaultParsers._
import java.util.concurrent.atomic.AtomicReference
import net.schmizz.sshj.SSHClient
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import org.pipifax.Utils
import Utils._
import org.pipifax.pidev.ssh.{SSH, SFTP}

object PiDev extends Plugin {
  self =>

  lazy val piDevSettings: Seq[Def.Setting[_]] = Seq(
    piAbsoluteTargetDirectory := {
      piMountDirectory.value.resolve(piTargetDirectory.value)
    },
    mountPi,
    umountPi,
    umountPiTask,
    isPiMounted,
    piJarsToCopy,
    sshTest,
    sshAuthFile := None,
    sshPiFingerprint := "",
    sshConnection,

    commands ++= Seq(runJavah, closeSSH, lsThere, copyToPi),
    loggerConfigFile := Keys.baseDirectory.value.toPath.resolve("src/main/resources/logback.xml"),
    configureLogging := resetLoggerConfiguration(loggerConfigFile.value),
    (onLoad in Global) := {
      val previous = (onLoad in Global).value
      val config = loggerConfigFile.value
      val reload: State => State = {
        state =>
          resetLoggerConfiguration(config)
          state
      }
      reload andThen previous
    },
    (onUnload in Global) := {
      val previous = (onUnload in Global).value
      closeSSHImpl andThen previous
    }

  )

  lazy val piMountDirectory = settingKey[Path]("Directory where deployment target on the target RaspberryPi is mounted")

  lazy val piMountCommand = settingKey[Seq[String]]("Command to mount the RaspberryPi")

  lazy val piUmountCommand = settingKey[Seq[String]]("Command to unmount the RaspberryPi")

  lazy val piTargetDirectory = settingKey[Path]("Target directory relative to piMountDirectory where created binaries should be stored")

  lazy val piSshProgramDirectory = settingKey[Path]("Target directory relative to the root directory when connecting to the RaspPerry pi with ssh")

  lazy val nativeCode = settingKey[Path]("Relative path to the C source code. Same path relative to piMountDirectory, project root directory and piSshProgramDirectory")
  lazy val compileCommand = settingKey[String]("Command to compile native sources. Only executable on the Pi")
  lazy val linkCommand = settingKey[String]("Command to create the shared library. Only executable on the Pi")
  lazy val piMainArgs = settingKey[String]("Arguments to main class")
  lazy val piRootMountPath = settingKey[String]("Root directory on the RaspberryPi that should be mounted")

  lazy val piAbsoluteTargetDirectory = settingKey[Path]("Absolute path on mounted RaspberryPi where binaries should be stored")

  lazy val loggerConfigFile = settingKey[Path]("Location of the logback config file")

  lazy val configureLogging = taskKey[Unit]("Reconfigures logback using loggerConfigFile setting")


  lazy val isPiMounted = taskKey[Boolean]("Checks if RaspberryPi is mounted") :=
      PiDev.checkPiMounted(piMountDirectory.value).get

  lazy val mountPi = taskKey[Path]("Mounts the RaspberryPi") := {
    if (isPiMounted.key.value) {
      piMountDirectory.value
    }
    else {
      val exitCode = executeCommand(streams.value.log, piMountCommand.value)
      require(exitCode == 0, "Executing mount command failed!")

      require(checkPiMounted(piMountDirectory.value).get, "Executing mount command had no effect")
      piMountDirectory.value
    }
  }

  lazy val umountPiTask = taskKey[Boolean]("Unmounts the RaspberryPi") := {
    require(!isPiMounted.key.value || executeCommand(streams.value.log, piUmountCommand.value) == 0)
    true
  }

  lazy val umountPi = taskKey[Unit]("Safely unmounts the RaspberryPi") := {
    require(umountPiTask.key.value && !checkPiMounted(piMountDirectory.value).get, "Unmounting had no effect!")
  }


  def evaluateSetting[T](state: State, key: SettingKey[T]) = {
    val extracted = Project.extract(state)

    key in extracted.currentRef evaluate extracted.structure.data
  }

  def doUmountPiChecked(logger: Logger, command: Seq[String]): Try[Boolean] = {
    if (executeCommand(logger, command) == 0) {
      Success(false)
    }
    else {
      Failure(new RuntimeException("Umount command failed: "))
    }
  }

  private def executeCommand(logger: Logger, command: Seq[String]): Int = {
    logger.info(s"Executing: ${ command mkString " " }")

    Process(command) ! logger
  }

  def resetLoggerConfiguration(configFile: Path): Unit = {
    if (Files.isRegularFile(configFile)) {
      val loggerContext = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext];
      val loggerConfigurator = new JoranConfigurator()
      loggerContext.reset()
      loggerConfigurator.setContext(loggerContext)
      loggerConfigurator.doConfigure(configFile.toString)
    }
  }


  def copyToPi = Command.command("copyToPi")(copyToPiAction)

  lazy val piJarsToCopy = taskKey[Seq[Path]]("Jar files to copy to the RaspberryPi. "
      + "Does not include native libraries to actually control GPIO ports") := {

    for(file <- (externalDependencyClasspath in Compile).value.files :+ (Keys.`package` in Compile).value)
    yield {
      file.toPath
    }
  }


  def eval[T](state: State, key: Def.ScopedKey[Task[T]]): (State, T) = {
    Project.runTask(key, state) match {
      case Some((state, Value(result))) => (state, result)
      case Some((state, Inc(failure))) => throw failure
      case None => throw new RuntimeException(s"Command $key not found")
    }
  }

  def copyToPiAction(state: State): State = {
    def eval[T](key: Def.ScopedKey[Task[T]]): (State, T) = self.eval(state, key)

    def evalSetting[T](key: SettingKey[T]): T = evaluateSetting(state, key)

    def flatEval[T](key: Def.ScopedKey[Task[T]]): T = eval(key)._2

    val result = Try {
      //      flatEval(mountPi.key)
      val sshControl = flatEval(sshConnection.key)

      val jarsToCopy = flatEval(piJarsToCopy.key)
      val relativeTargetDir = evalSetting(piTargetDirectory)

      val copyCount = copyFilesAction(sshControl, state.log, jarsToCopy, relativeTargetDir)

      state.log.info(s"$copyCount of ${ jarsToCopy.size } files copied to $relativeTargetDir")


      val classPath = jarsToCopy map {
        p =>
          val name = p.getFileName.toString
          if (name contains " ") {
            "\"" + name + "\""
          }
          else {
            name
          }
      } mkString ":"
      val mainClass = flatEval(Keys.mainClass).get
      val mainArgs = evalSetting(piMainArgs)


      createShellscript(
        sshControl,
        targetDir = relativeTargetDir,
        name="execute",
        commands=Seq(
          s"java -cp $classPath $mainClass $mainArgs"
        ))

      createShellscript(sshControl, relativeTargetDir, "execute", Seq(
        s"java -cp $classPath $mainClass $mainArgs\n"
      ))



      val projectPath = evalSetting(thisProject).base.toPath
      val relativeNativeDir: Path = evalSetting(nativeCode)
      val nativePath = projectPath.toAbsolutePath.resolve(relativeNativeDir)
      val targetNativePath = relativeTargetDir.resolve(relativeNativeDir)
      sshControl.execute("rm -r " + targetNativePath.toString)
      sshControl.execute("mkdir " + targetNativePath.toString)
      sshControl.execute("chmod a+rwx " + targetNativePath.toString)
      sshControl.uploadDirectory(nativePath, targetNativePath.toString)
      createShellscript(sshControl, relativeTargetDir, "compile", Seq(
        "rm *.o",
        "rm *.so",
        s"cp ~/bcm2835-1.36/src/bcm2835.o ${targetNativePath.toUnixPath}",
        s"pushd ${targetNativePath.toUnixPath}",
        evalSetting(compileCommand),
        evalSetting(linkCommand),
        "popd"
      ))

      sshControl.execute(s"${toUnixPath(relativeTargetDir / "compile")}")

      state
    }

    newState(state, result)
  }

  def createShellscript(sshControl: SSHControl, targetDir: Path, name: String, commands:Seq[String]) = {
    val executable: Path = System.getProperty("java.io.tmpdir").nioPath / name
    using(Files.newBufferedWriter(executable,
      Charset.forName("UTF-8"),
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING)) {
      out =>

        out.write("#/bin/sh\n\n");
        for (c <- commands) {
          out.write(c)
          out.write('\n')
        }
        out.flush()
    }
    sshControl.uploadFile(executable, targetDir.toUnixPath)
    sshControl.execute("chmod a+x " + toUnixPath(targetDir / name))
  }

  def copyFilesAction(sshControl: SSHControl, logger: Logger, sources: Seq[Path], target: Path): Int = {
    sources.foldLeft(0) {
      (copyCount, source) =>
        sshControl.uploadFiles(source.getParent, Seq(source), target.toString)
        copyCount + 1
    }
  }

  def copyFile(logger: Logger, file: Path, targetFile: Path): Boolean = {
    if (Files.exists(targetFile) && Files.getLastModifiedTime(targetFile).compareTo(Files.getLastModifiedTime(file)) >= 0) {
      false
    }
    else {
      logger.info(s"Copying: $file  -->  $targetFile")
      Files.copy(file,
        targetFile,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.COPY_ATTRIBUTES)
      true
    }
  }


  private def checkPiMounted(mountDirectory: Path): Try[Boolean] = {
    val files = mountDirectory.toFile.listFiles()
    if (files == null) {
      Failure(new RuntimeException(s"$mountDirectory is not a directory!"))
    }
    else if (files.length == 0) {
      Success(false)
    }
    else {
      Success(true)
    }
  }


  private lazy val javahAction: (State, Seq[String]) => State = {
    (state, args) =>

      val newState = for {
        (state, taskResult) <- Project.runTask(fullClasspath in Compile, state)
      } yield {
        taskResult.toEither match {
          case Left(failure) =>
            Incomplete.show(failure.tpe)
            state.fail
          case Right(attributed) =>
            val classPathString = attributed.files mkString ":"

            val command = Seq("javah") ++ args ++ Seq("-classpath", classPathString, "org.pipifax.gpio.GpioNative")

            state.log.info(s"Executing: ${ command mkString " " }")

            val process = Process(command)
            val result = process ! state.log
            if (result == 0) {
              state
            } else {
              state.log.error("Javah finished with " + result)
              state.fail
            }
        }
      }

      newState getOrElse state.fail
  }

  private def newState(state: State, result: Try[_], message: String = ""): State = result match {
    case Success(_) =>
      if (!message.isEmpty) {
        state.log.success(message)
      }
      else {
        state.log.success("Command finished successfully")
      }
      state
    case Failure(ex) =>
      state.log.error(ex.toString)
      state.log.trace(ex)

      state.copy(remainingCommands = Seq()).fail
  }


  private lazy val javahArgParser = {
    val verbose = token(Space ~> "-verbose")
    val force = token(Space ~> "-force")
    val javahArgs = (verbose | force).*
    javahArgs
  }


  def runJavah = Command("runJavah")(_ => javahArgParser)(javahAction)


  /*
   * Copied from sbt documentation.
   */
  // A command that demonstrates getting information out of State.
  def printState = Command.command("printState") {
    state =>
      import state._
      println(definedCommands.size + " registered commands")
      println("commands to run: " + show(remainingCommands))
      println()

      println("original arguments: " + show(configuration.arguments))
      println("base directory: " + configuration.baseDirectory)
      println()

      println("sbt version: " + configuration.provider.id.version)
      println("Scala version (for sbt): " + configuration.provider.scalaProvider.version)
      println()

      val extracted = Project.extract(state)
      import extracted._
      println("Current build: " + currentRef.build)
      println("Current project: " + currentRef.project)
      println("Original setting count: " + session.original.size)
      println("Session setting count: " + session.append.size)

      state
  }


  def show[T](s: Seq[T]) =
    s.map("'" + _ + "'").mkString("[", ", ", "]")


  lazy val sshjConnection: AtomicReference[SSHControl] = new AtomicReference()


  lazy val sshTest = taskKey[Unit]("Test sshj") := {
    sshjConnect()
  }

  lazy val sshHost = settingKey[String]("host name of the raspberry pi")

  lazy val sshUser = settingKey[String]("User name to log in on the RaspberryPi")

  lazy val sshAuthFile = settingKey[Option[Path]]("Path to ssh private key file, when not id_rsa or id_dsa can be used.")

  lazy val sshPiFingerprint = settingKey[String]("Fingerprint of the ssh server on the RaspberryPi when local known_hosts file cannot be used")

  lazy val sshConnection = taskKey[SSHControl]("Internally managed SSH connection to the Raspberry Pi") := {
    val curConn = sshjConnection.get()
    if (curConn != null) {
      curConn
    }
    else {
      val newConn = new SSHControl(
        hostName = sshHost.value,
        userName = sshUser.value,
        authenticationFile = sshAuthFile.value.get,
        fingerPrint = sshPiFingerprint.value,
        logger = streams.value.log)

      assume(sshjConnection.compareAndSet(null, newConn))
      newConn
    }
  }


  def closeSSH = Command.command("closeSSH")(closeSSHImpl)


  lazy val closeSSHImpl: State => State = {
    state =>
      val sshControl = sshjConnection.get()
      if (sshControl ne null) sshControl.close()

      state
  }

  def lsThere = Command.command("lsThere") {
    state =>
      val (state2, control) = eval(state, sshConnection.key)
      val succ = Try(control.execute("ls -l gpioControl/*"))

      newState(state2, succ)
  }

  def sshjConnect(): Unit = {
    println("v1")
    val ssh = new SSHClient()
    ssh.loadKnownHosts(file("/home/andi/.ssh/known_hosts"))
    ssh.addHostKeyVerifier("01:b7:6f:c9:14:b7:7b:30:7f:5c:82:55:4f:cb:24:6b")
    ssh.connect("pipifax")
    try {
      ssh.authPublickey("pi", "/home/andi/.ssh/pi2_rsa");
      using(ssh.newSFTPClient()) {
        ftp =>
          val ls = ftp.ls("/")
          ls.asScala.foreach(println _)
      }
    } finally {
      ssh.disconnect()
    }
  }

}