package org.pipifax.monitor

import org.scalatest.{Spec, Matchers}
import Matchers._

class MainSpec extends Spec {

  val simpleConfig =
    """
      |jenkinsRoot=http://localhost:8080/jenkins
      |controlFile=/tmp/pipifax.ctrl
    """.stripMargin
  object `Main clazz` {
    def `should load properties` {
      val prop = Main.loadConfiguration(new java.io.ByteArrayInputStream(simpleConfig.getBytes("ASCII")))

      prop.size should be (2)
      prop("jenkinsRoot") shouldBe "http://localhost:8080/jenkins"
      prop("controlFile") shouldBe "/tmp/pipifax.ctrl"
    }

  }
}
