package test_utils

import play.api.libs.json.{JsValue, Json}

import scala.io.Source

trait ResourceProvider {

  val exampleDwpRequest: String => JsValue = nino => readJson("./it/resources/example-dwp-request.json", nino)


  private def readJson(path: String, nino: String): JsValue =
    Json.parse(Source.fromFile(path).mkString.replace("QQ123456C",  nino))
}
