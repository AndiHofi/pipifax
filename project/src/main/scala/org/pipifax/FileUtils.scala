package org.pipifax

import java.nio.file.{StandardOpenOption, Files, Path}
import Utils._
import java.security.MessageDigest
import scala.annotation.tailrec
import org.bouncycastle.crypto.digests.SHA1Digest
import java.math.BigInteger


object FileUtils {
  final val BUFFER_SIZE = 8*1024

  @tailrec
  def lowLevelDigest(in: java.io.InputStream, buf: Array[Byte], digest: SHA1Digest): Array[Byte] = {
    val bytesRead = in.read(buf, 0, buf.length)
    if (bytesRead > 0) {
      digest.update(buf, 0, bytesRead)
      lowLevelDigest(in, buf, digest)
    } else {
      val result = new Array[Byte](digest.getDigestSize)
      digest.doFinal(result, 0)
      result
    }
  }

  @tailrec
  def updateDigest(in: java.io.InputStream, buf: Array[Byte], digest: MessageDigest): Array[Byte] = {
    val bytesRead = in.read(buf, 0, buf.length)
    if (bytesRead > 0) {
      digest.update(buf, 0, bytesRead)
      updateDigest(in, buf, digest)
    } else {
      digest.digest
    }
  }

  def calcSHA1(file: Path): String = {
    require(Files.isRegularFile(file))
      val digest = new SHA1Digest()


    val hash = using(Files.newInputStream(file, StandardOpenOption.READ)) {in =>
      val buf = new Array[Byte](BUFFER_SIZE)
      lowLevelDigest(in, buf, new SHA1Digest())
    }

    sha1ToHex(hash)
  }



  def sha1ToHex(hash: Array[Byte]): String = {
    new BigInteger(hash).toString(16).padLeft(40, '0')
  }
}
