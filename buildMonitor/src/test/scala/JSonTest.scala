import org.scalatest.Spec
import org.scalatest.Matchers._

import spray.json._
import DefaultJsonProtocol._

import scala.collection.immutable.Map

/**
 * Created by andi on 23.01.14.
 */
class JSonTest extends Spec {

  object `Basic spray-json usage` {
    val str =
      """
        |{"Build": {
        |    "name":"MyBuild",
        |    "executions": [
        |        {"execution": {"time":"2014-01-11T12:15:22", "result":"red", "buildNumber": 4}},
        |        {"execution":{"time":"2014-01-11T12:25:10", "result":"green", "buildNumber": 5}}]
        |}}
      """.stripMargin

    def `should parse string` {

      val json: JsValue = str.asJson

      val nameValue: JsValue = json.asJsObject().fields("Build").asJsObject.fields("name")
      val name = nameValue.convertTo[String]

      name shouldBe "MyBuild"
    }

    def `should extract with pattern matcher` {

      implicit class JsonExtractor(js: JsValue) {
        def \\(key: String) = js match {
          case JsObject(map) => map.get(key)
          case _ => None
        }

        def asList = js match {
          case JsArray(list) => list
          case _ => throw new ClassCastException("Not a JsArray: " + js.getClass + " -- " + js)
        }

        def flatMap[B](f: JsValue => scala.collection.TraversableOnce[B]) = asList.flatMap(f)
      }

      implicit class OptionJsonExtractor(js: Option[JsValue]) {
        def \\(key: String) = js match {
          case Some(JsObject(map)) => map.get(key)
          case _ => None
        }

        def apply(index: Int) = js match {
          case Some(JsArray(list)) =>
            list.drop(index).headOption
          case _ => None
        }



        def asList = js match {
          case Some(JsArray(list)) => list
          case _ => throw new ClassCastException("Not a JsArray: " + js)
        }
      }

      case class Execution(timestamp: String, buildNumber: Int, success: Boolean)

      implicit val colorFormat = jsonFormat3(Execution.apply)


      val json = str.asJson

      val results = for {
        executionArray <- (json \\ "Build" \\ "executions").toList
        exec <- executionArray
        exec2 <- exec \\ "execution"
        buildNumber <- exec2 \\ "buildNumber"
        color <- exec2 \\ "result"
      } yield buildNumber.convertTo[Int] -> (color.convertTo[String] == "green")

      results should equal (List(4 -> false, 5 -> true))

    }

    def `should extract with protocol` {

      implicit class JsonExtractor(js: JsValue) {
        def \\(key: String) = js match {
          case JsObject(map) => map.get(key)
          case _ => None
        }

        def asList = js match {
          case JsArray(list) => list
          case _ => throw new ClassCastException("Not a JsArray: " + js.getClass + " -- " + js)
        }

        def flatMap[B](f: JsValue => scala.collection.TraversableOnce[B]) = asList.flatMap(f)
      }

      implicit class OptionJsonExtractor(js: Option[JsValue]) {
        def \\(key: String) = js match {
          case Some(JsObject(map)) => map.get(key)
          case _ => None
        }

        def apply(index: Int) = js match {
          case Some(JsArray(list)) =>
            list.drop(index).headOption
          case _ => None
        }



        def asList = js match {
          case Some(JsArray(list)) => list
          case _ => throw new ClassCastException("Not a JsArray: " + js)
        }
      }

      case class Execution(time: String, result: String, buildNumber: Int)

      implicit val colorFormat = jsonFormat3(Execution.apply)


      val json = str.asJson

      val results = for {
        executionArray <- (json \\ "Build" \\ "executions").toList
        exec <- executionArray
        exec2 <- exec \\ "execution"
      } yield exec2.convertTo[Execution]

      results should equal (List(Execution("2014-01-11T12:15:22", "red", 4), Execution("2014-01-11T12:25:10", "green", 5)))
    }
  }


}
