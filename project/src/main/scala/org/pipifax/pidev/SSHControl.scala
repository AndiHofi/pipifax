package org.pipifax.pidev

import ssh._
import java.nio.file.{Paths, Files, Path}
import java.util.concurrent.{TimeUnit, Semaphore}
import sbt.Logger
import org.pipifax.Utils._

class SSHControl(hostName: String,
                 userName: String,
                 authenticationFile: Path,
                 fingerPrint: String,
                 logger: Logger
                    ) {


  final val ConnectionCount = 2

  require(!hostName.isEmpty)
  require(!userName.isEmpty)
  require(Files.isRegularFile(authenticationFile))
  require(!fingerPrint.isEmpty)


  private[this] var __connections: List[SSH] = Nil
  private[this] var __usedConnections = new Semaphore(ConnectionCount)
  private[this] var __sftp: Map[SSH, SFTP] = Map.empty


  def close() {
    __usedConnections.acquire(ConnectionCount)
    try {
      __connections foreach {
        c =>
          __sftp.get(c) foreach { _.close() }
          c.close()
      }
      __connections = Nil
    } finally __usedConnections.release(ConnectionCount)
  }

  def listFiles = execute("ls")

  def execute(cmd: String) = withConnection { _.execute(cmd) }


  def uploadDirectory(src: Path, target: String) = withConnection {
    c =>
      logger.info(s"Uploading directory $src to $target")
      val sftp: SFTP = getSFTPConnection(c)

      sftp.copyDirectoryTo(src, target)
      logger.debug(s"Uploaded directory $src to $target")
  }

  def uploadFile(src: Path, targetDir: String) = withConnection {
    c =>
      c.uploadSingle(src, targetDir)
  }

  def uploadExecutableFile(src: Path, targetDir: String) = withConnection {
    c =>
      c.uploadSingle(src, targetDir)
      c.execute("chmod a+x targetDir")
  }

  def uploadFiles(basePath: Path, sources: Seq[Path], targetDir: String) = withConnection {
    c =>

      def calcTargetFile(src: Path): Path = {
        val relName = basePath.relativize(src)
        Paths.get(targetDir).resolve(relName)
      }

      def calcTargetDir(src: Path): String = {
        val targetFileName = calcTargetFile(src)

        if (targetFileName.getNameCount < 2) {
          "."
        }
        else {
          targetFileName.getParent.toString
        }
      }
      if (sources.isEmpty) {
        logger.warn("No source files provided!")
      } else if (sources.length < 1) {
        logger.debug("Using scp transfer")
        for(src <- sources) {
          val targetName: String = calcTargetDir(src)
          logger.info(s"Uploading file $src to $targetName")
          c.uploadSingle(src, targetName)
        }
      } else {
        logger.debug("Using sftp transfer")
        val sftp = getSFTPConnection(c)
        for(src <- sources) {
          val targetDir = calcTargetDir(src)
          val mtime = sftp.sftpClient.mtime(calcTargetFile(src).toUnixPath)
          val remoteMtime: Long = TimeUnit.SECONDS.toMinutes(mtime)
          val localMtime: Long = Files.getLastModifiedTime(src).to(TimeUnit.MINUTES)
          if (remoteMtime < localMtime) {
            logger.info(s"Uploading file $src to $targetDir --- $remoteMtime < $localMtime")
            sftp.copyFileTo(src, targetDir)
          } else {
            logger.info(s"file $src alread up-to-date")
          }
        }
      }
  }


  def getSFTPConnection(c: SSH): SFTP = synchronized {
    __sftp.get(c) match {
      case Some(sftp) => sftp
      case None =>
        val sftp: SFTP = c.openSftp()
        __sftp = __sftp.updated(c, sftp)
        sftp
    }
  }

  private def withConnection[A](f: SSH => A): A = {
    val conn = fetchConnection()
    try {
      f(conn)
    } finally {
      releaseConnection(conn)
    }
  }


  private[this] def fetchConnection(): SSH = synchronized {
//    println(s"Free connections ${ __usedConnections.availablePermits() }")
    __usedConnections.acquire()
    if (__connections.isEmpty) {
      SSH.connect(hostName = hostName, userName = userName, authenticationFile = Some(authenticationFile), fingerPrint = fingerPrint)
    } else {
      val conn = __connections.head
      __connections = __connections.tail
      conn
    }
  }

  private[this] def releaseConnection(conn: SSH): Unit = synchronized {
    if (conn.isConnected) {
      __connections ::= conn
    }
    __usedConnections.release()
//    println(s"Free connections ${ __usedConnections.availablePermits() }")
  }
}



