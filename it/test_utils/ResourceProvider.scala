package test_utils

import play.api.libs.json.Json

import scala.io.{BufferedSource, Source}

trait ResourceProvider {

  val exampleDwpRequest = Json.toJson(loadFile("./it/resources/example-dwp-request.json"))

  private def loadFile(name: String): String = {
    var source: BufferedSource = null
    try {
      source = Source.fromFile(name)
      source.mkString
    } finally {
      if(source != null)
        source.close()
    }
  }
}
