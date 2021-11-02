package test_utils

import play.api.libs.json.{JsValue, Json}

import scala.io.Source

trait ResourceProvider {

  def dwpRequest(nino: String): JsValue = getRequest("dwp-request", nino)

  def fullDesResponse(nino: String): JsValue = readJson(s"./it/resources/full-des-response.json", nino)

  def getRequest(fileName: String, nino: String): JsValue = readJson(s"./it/resources/requests/$fileName.json", nino)

  def getResponse(fileName: String, nino: String): JsValue = readJson(s"./it/resources/responses/$fileName.json", nino)

  private def readJson(path: String, nino: String): JsValue = {
    val bufferedSource = Source.fromFile(path)
    val json = Json.parse(bufferedSource.mkString.replace("QQ123456C",  nino))
    bufferedSource.close
    json
  }
}
