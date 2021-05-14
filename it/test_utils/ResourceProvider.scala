package test_utils

import play.api.libs.json.{JsValue, Json}

import scala.io.Source

trait ResourceProvider {

  val exampleDwpRequest: JsValue = readJson("./it/resources/example-dwp-request.json")

  private def readJson(path: String): JsValue =
    Json.parse(Source.fromFile(path).mkString)
}
