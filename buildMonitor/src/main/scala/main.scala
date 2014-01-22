package org.pipifax.monitor

import java.io.{FileInputStream, InputStream, File}
import java.util.Properties
import utils._

import scala.collection.JavaConverters._
import scala.collection.immutable.Map

object Main extends App {

  val configLocation =  if (args.length == 1) new File(args(0)) else inUserHome()

  require(configLocation.isFile, s"No configuration found at $configLocation")

  val configuration = loadConfiguration(configLocation)

  for ((key, value) <- configuration) println(s"$key -> $value")



  def inUserHome() = {
    val home = new File(System.getProperty("user.home"))

    new File(home, ".pipifax.config")
  }

  def loadConfiguration(location: File): Map[String, String] = {
    using(new java.io.FileInputStream(location)) {loadConfiguration(_)}

  }

  def loadConfiguration(source: InputStream): Map[String, String] = {
    val prop = new Properties()
    prop.load(source)
    prop.asScala.toMap
  }

}

