name := "pipifax"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.3"

lazy val root =  project.in(file("."))
  .aggregate(buildMonitor, gpioControl)


lazy val buildMonitor = project

lazy val gpioControl = project