package org.pipifax.pidev

import org.scalatest.{BeforeAndAfter, Matchers, Spec}
import Matchers._
import org.pipifax.pidev.ssh.SSH
import org.pipifax.Utils._
import java.nio.file.{Path, Files, Paths}


class SshjTest extends Spec with BeforeAndAfter {
  var connection: SSH = _

//  object `Connecting SSH` {
//    def `should create connection` {
//      1 shouldBe 1
//      using(connectToPi()) { conn =>
//          conn.isConnected shouldBe true
//
//          conn.disconnect()
//
//          conn.isConnected shouldBe false
//      }
//    }
//  }

  before {
    connection = connectToPi()
  }

  after {
    if (connection != null) {
      connection.close()
    }
  }

  object `Using SSH` {
    def `should execute commands` {
      connection.execute("gpio export 4 out")
      connection.execute("gpio -g write 4 1")
      connection.execute("gpio -g write 4 0")
      connection.execute("gpio reset")
    }

    def `should execute reading command` {
      val (status, out, err) =connection.executeAndGet("pwd")
      status shouldBe 0
      out shouldBe "/home/pi\n"
      err shouldBe ""
    }

    def `should read whole gpio status` {
      val (status, out, err) = connection.executeAndGet("gpio readAll")
      status shouldBe 0
      err shouldBe ""
      println(out)
    }

    def `should allow executing remote script file` {
      val script =
        """
          |#!/bin/sh
          |
          |gpio export 17 out
          |gpio -g write 17 1
          |gpio export 18 out
          |gpio -g write 18 1
          |gpio -g write 17 0
          |gpio export 27 out
          |gpio -g write 27 1
          |gpio -g write 18 0
          |gpio export 22 out
          |gpio -g write 22 1
          |gpio -g write 27 0
          |gpio export 25 out
          |gpio -g write 25 1
          |gpio -g write 22 0
          |gpio export 4 out
          |gpio -g write 4 1
          |gpio -g write 25 0
          |gpio -g write 4 0
          |gpio reset
          |
        """.stripMargin

      val tmpFile: Path = Paths.get("/tmp/testScript")
      Files.write(tmpFile, script.getBytes("ASCII"))
      connection.uploadSingle(tmpFile, ".")
//      val sftp = connection.openSftp()
//
//      sftp.copyFileTo(tmpFile)
//      sftp.close()
      connection.execute("chmod a+x testScript")
      connection.execute("./testScript")

    }
  }

  def connectToPi(): SSH = {
    SSH.connect(
      userName = "pi",
      hostName = "pipifax",
      authenticationFile = Some(Paths.get("/home/andi/.ssh/pi2_rsa")),
      fingerPrint = "01:b7:6f:c9:14:b7:7b:30:7f:5c:82:55:4f:cb:24:6b")
  }
}