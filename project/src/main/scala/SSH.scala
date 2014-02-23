package org.pipifax.pidev.ssh
import net.schmizz.sshj.SSHClient
import java.nio.file._
import net.schmizz.sshj.common.{IOUtils, StreamCopier}
import java.util.concurrent.TimeUnit

object SSH {
  def connect(hostName: String,
              userName: String,
              authenticationFile: Option[Path] = None,
              fingerPrint: String = ""): SSH = {
    require(!hostName.isEmpty)

    val client = new SSHClient()

    if (fingerPrint.isEmpty) {
      client.loadKnownHosts()
    } else {
      client.addHostKeyVerifier(fingerPrint)
    }

    client.connect(hostName)
    try {
      authenticationFile match {
        case Some(path) =>
          client.authPublickey(userName, path.toString)
        case None =>
          client.authPublickey(userName)
      }

      new SSH(client)
    } catch {
      case e: Exception =>
        client.disconnect()
        throw e
    }
  }
}

class SSH(private[ssh] val sshClient: SSHClient) extends AnyRef with AutoCloseable {
  def isConnected = sshClient.isConnected

  def openSftp(rootDir: String = "~/") = SFTP(this, rootDir = rootDir)

  def isFTPConnected(sftp: SFTP) = (sftp.client eq this) && sshClient.isConnected

  def disconnect() = sshClient.disconnect()

  def execute(command: String): Int = {
    val session = sshClient.startSession()
    val execution = session.exec(command)
    System.out.println(IOUtils.readFully(execution.getInputStream))
    System.err.println(IOUtils.readFully(execution.getErrorStream))
    execution.join(10, TimeUnit.SECONDS)
    System.out.println(s"Result: ${execution.getExitStatus}")
    execution.getExitStatus
  }

  def uploadSingle(src: Path, dst: String): Unit = {
    require(Files.isRegularFile(src))

    val transfer = sshClient.newSCPFileTransfer()
    transfer.upload(src.toString, dst)
  }

  def executeAndGet(command: String) = {
    val session = sshClient.startSession()
    val execution = session.exec(command)
    val bufferStream = IOUtils.readFully(execution.getInputStream)
    val errorBufferStream = IOUtils.readFully(execution.getErrorStream)
    execution.join(10, TimeUnit.SECONDS)
    (execution.getExitStatus, new String(bufferStream.toByteArray, "ASCII"), new String(errorBufferStream.toByteArray, "ASCII"))
  }

  override def close(): Unit = {
    disconnect()
  }

}