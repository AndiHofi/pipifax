package org.pipifax.pidev.ssh

import net.schmizz.sshj.sftp.{FileAttributes, RemoteResourceInfo, SFTPClient}
import scala.collection.JavaConverters._
import java.nio.file.{Paths, Files, Path}
import org.pipifax.Utils._

object SFTP {
  private[ssh] def apply(client: SSH, rootDir: String = "~/"): SFTP = {
    val sftpClient: SFTPClient = client.sshClient.newSFTPClient()
    sftpClient.getFileTransfer.setPreserveAttributes(true)

    new SFTP(client, sftpClient, rootDir)
  }
}

class SFTP(val client: SSH, val sftpClient: SFTPClient, initCwd: String = "/") extends AnyRef with AutoCloseable {


  def copyFileTo(src: Path, dst: String = "."): Unit = {
    require(Files.isRegularFile(src))

    val transfer = sftpClient.getFileTransfer
    val target: String = dst
    println(s"Copying $src to $target")
    transfer.upload(src.toString, target)
//    sftpClient.setattr(target, FileAttributes.EMPTY)
  }

  def copyDirectoryTo(src: Path, dst: String = ".") = {
    require(Files.isDirectory(src))
    println(s"Uploading directory $src to ${ dst }")
    Thread.sleep(5000)
    sftpClient.put(src.toString, dst)
  }

  def copyDirectoryFrom(src: String, dst: Path) {
    require(Files.isDirectory(dst))
    sftpClient.getFileTransfer.download(src, dst.toString)
  }

  def isLocalFileNewer(src: Path, dst: String) = {
    val attributes = sftpClient.lstat(dst)
    val lastModified = Files.getLastModifiedTime(src)

    val remoteLastModified = attributes.getMtime
    lastModified.toMillis > remoteLastModified
  }

  override def close(): Unit = sftpClient.close()
}