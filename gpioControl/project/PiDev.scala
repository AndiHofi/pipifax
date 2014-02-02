
import sbt._
import sbt.Inc
import sbt.Keys._
import sbt.Task
import sbt.Value
import scala.Some
import scala.util.{Try, Success, Failure}
import java.nio.file._
import java.nio.file.attribute.{BasicFileAttributes}
import java.nio.charset.Charset
import complete.DefaultParsers._

object PiDev extends Build {
  lazy val piMountDirectory = settingKey[Path]("Directory where deployment target on the target RaspberryPi is mounted")

  lazy val piMountCommand = settingKey[Seq[String]]("Command to mount the RaspberryPi")

  lazy val piUmountCommand = settingKey[Seq[String]]("Command to unmount the RaspberryPi")

  lazy val piTargetDirectory = settingKey[Path]("Target directory relative to piMountDirectory where created binaries should be stored")

  lazy val piSshProgramDirectory = settingKey[Path]("Target directory relative to the root directory when connecting to the RaspPerry pi with ssh")

  lazy val nativeCode = settingKey[Path]("Relative path to the C source code. Same path relative to piMountDirectory, project root directory and piSshProgramDirectory")
  lazy val compileCommand = settingKey[String]("Command to compile native sources. Only executable on the Pi")
  lazy val linkCommand = settingKey[String]("Command to create the shared library. Only executable on the Pi")
  lazy val piMainArgs = settingKey[String]("Arguments to main class")

  lazy val piAbsoluteTargetDirectory = taskKey[Path]("Absolute path on mounted RaspberryPi where binaries should be stored") := {
   piMountDirectory.value.resolve(piTargetDirectory.value)
  }

  lazy val isPiMounted = taskKey[Boolean]("Checks if RaspberryPi is mounted") :=
    PiDev.checkPiMounted(piMountDirectory.value).get

  lazy val mountPi = taskKey[Path]("Mounts the RaspberryPi") := {
    if (isPiMounted.key.value) piMountDirectory.value
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
    if (executeCommand(logger, command) == 0) Success(false)
    else Failure(new RuntimeException("Umount command failed: "))
  }

  private def executeCommand(logger: Logger, command: Seq[String]): Int = {
    logger.info(s"Executing: ${command mkString " "}")

    Process(command) ! logger
  }


  def copyToPi = Command.command("copyToPi")(copyToPiAction)

  val piJarsToCopy = taskKey[Seq[Path]]("Jar files to copy to the RaspberryPi. "
    +"Does not include native libraries to actually control GPIO ports") := {

      for (file <- (externalDependencyClasspath in Compile).value.files :+ (Keys.`package` in Compile).value)
        yield file.toPath
  }


  def copyToPiAction(state: State): State = {
    def eval[T](key: Def.ScopedKey[Task[T]]): (State, T) = {
      Project.runTask(key, state) match {
        case Some((state, Value(result))) => (state, result)
        case Some((state, Inc(failure))) => throw failure
        case None => throw new RuntimeException(s"Command $key not found")
      }
    }
    def evalSetting[T](key: SettingKey[T]): T = evaluateSetting(state, key)

    def flatEval[T](key: Def.ScopedKey[Task[T]]): T = eval(key)._2

    val result = Try {
      flatEval(mountPi.key)

      val jarsToCopy = flatEval(piJarsToCopy.key)
      val targetDir = flatEval(piAbsoluteTargetDirectory.key)
      val copyCount = copyFilesAction(state.log, jarsToCopy, targetDir)
      state.log.info(s"$copyCount of ${jarsToCopy.size} files copied to $targetDir")


      val classPath = jarsToCopy map {p =>
        val name = p.getFileName.toString
        if (name contains " ") "\""+name+"\""
        else name
      } mkString ":"
      val mainClass = flatEval(Keys.mainClass).get
      val mainArgs = evalSetting(piMainArgs)

      val executable: Path = targetDir.resolve("execute")
      using(Files.newBufferedWriter(executable,
        Charset.forName("UTF-8"),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)) { out =>

        out.write("#/bin/sh\n\n");
        val javaCmd = s"java -cp $classPath $mainClass $mainArgs\n"
        out.write(javaCmd)
        state.log.debug(javaCmd)
        out.flush()
      }
      state.log.info("Created main executable at " + executable)

      import java.nio.file.attribute.PosixFilePermissions
      Files.setPosixFilePermissions(executable, PosixFilePermissions.fromString("rwxr-x---"))

      val projectPath = evalSetting(thisProject).base.toPath
      val relativeNativeDir: Path = evalSetting(nativeCode)
      val nativePath = projectPath.toAbsolutePath.resolve(relativeNativeDir)
      val targetNativePath = targetDir.resolve(relativeNativeDir)

      Files.walkFileTree(nativePath, new SimpleFileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
          val relative = nativePath.relativize(dir)
          val onTarget = targetNativePath.resolve(relative)
          if (!Files.isDirectory(onTarget)) Files.createDirectories(onTarget)
          FileVisitResult.CONTINUE
        }

        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          val relative = nativePath.relativize(file)
          val onTarget = targetNativePath.resolve(relative)
          copyFile(state.log, file, onTarget)
          FileVisitResult.CONTINUE
        }
      })

      state
    }

    newState(state, result)
  }

  def copyFilesAction(logger: Logger, sources: Seq[Path], target: Path): Int = {
    sources.foldLeft(0) { (copyCount, source) =>
      if (copyFile(logger, source, target.resolve(source.getFileName.toString()))) copyCount + 1
      else copyCount
    }
  }

  def copyFile(logger: Logger, file: Path, targetFile: Path): Boolean = {
    if (Files.exists(targetFile) && Files.getLastModifiedTime(targetFile).compareTo(Files.getLastModifiedTime(file)) >= 0) false
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
    if (files == null) Failure(new RuntimeException(s"$mountDirectory is not a directory!"))
    else if (files.length == 0) Success(false)
    else Success(true)
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

            state.log.info(s"Executing: ${command mkString " "}")

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
      if (!message.isEmpty) state.log.success(message)
      else state.log.success("Command finished successfully")
      state
    case Failure(ex) =>
      state.log.error(ex.toString)
      state.log.trace(ex)
      state.fail
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

  def using[A <: AutoCloseable, B](resource: A)(f: A => B): B = {
    if (resource eq null) throw new NullPointerException()

    val result: B =
      try {
        f(resource)
      } catch {
        case th: Throwable =>
          try {
            resource.close()
          } catch {
            case onClose: Throwable =>
              th.addSuppressed(onClose)
          }
          throw th
      }

    resource.close()

    result
  }

  def show[T](s: Seq[T]) =
    s.map("'" + _ + "'").mkString("[", ", ", "]")

}