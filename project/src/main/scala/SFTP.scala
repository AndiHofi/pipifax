package org.pipifax.pidev.ssh

import net.schmizz.sshj.sftp.{RemoteResourceInfo, SFTPClient}
import scala.collection.JavaConverters._
import java.nio.file.{Paths, Files, Path}

object SFTP {
  private[ssh] def apply(client: SSH, rootDir: String = "~/"): SFTP = {
    val sftpClient: SFTPClient = client.sshClient.newSFTPClient()
    sftpClient.getFileTransfer.setPreserveAttributes(true)

    new SFTP(client, sftpClient, rootDir)
  }
}

class SFTP (val client: SSH, sftpClient: SFTPClient, initCwd: String = "/") extends AnyRef with AutoCloseable {
  @volatile private var cwd: Path = Paths.get(initCwd)

  def ls: Seq[RemoteResourceInfo] = {
    val jlist = sftpClient.ls(cwd.toString)

    jlist.asScala
  }

  def copyFileTo(src: Path, dst: String = "."): Unit = {
    require(Files.isRegularFile(src))

    val transfer = sftpClient.getFileTransfer
    val target: String = dst
    println(s"Copying $src to $target")
    transfer.upload(src.toString, target)
  }

  def copyDirectoryTo(src: Path, dst: String = ".") = {
    require(Files.isDirectory(src))
    sftpClient.put(src.toString, cwd.resolve(dst).toString)
  }

  def copyDirectoryFrom(src: String, dst: Path) {
    require(Files.isDirectory(dst))
    sftpClient.getFileTransfer.download(cwd.resolve(src).toString, dst.toString)
  }

  def isLocalFileNewer(src: Path, dst: String) = {
    val attributes = sftpClient.lstat(cwd.resolve(dst).toString)
    val lastModified = Files.getLastModifiedTime(src)

    val remoteLastModified = attributes.getMtime
    lastModified.toMillis > remoteLastModified
  }

  override def close(): Unit = sftpClient.close()
}