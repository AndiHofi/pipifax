name := "pipifax.monitor"

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.2.5",
  "io.spray" % "spray-http" % "1.2.0",
  "org.scalatest" %% "scalatest" % "2.0" % "test"
)

//libraryDependencies in test ++= Seq(
//  "org.scalatest" %% "scalatest" % "2.0"
//)



