
import play.api.libs.json.{JsValue, Json, __}

import scala.io.Source

case class DesSuccessResponse(matchPattern: Int, taxYears: Option[List[JsValue]])
object DesSuccessResponse {
  implicit val formats = Json.format[DesSuccessResponse]
}

def readJson(path: String) = {
  val resource = getClass.getResourceAsStream(path)
  Json.parse(Source.fromInputStream(resource).getLines().mkString)
}

def pickOneValue(key: String, taxYear: JsValue): Option[(String, JsValue)] = {
  taxYear.transform((__ \\ key).json.pick[JsValue]).asOpt match {
    case Some(x) => Some(key -> x)
    case None => None
  }
}

def pickAll(keys: List[String], desSuccessResponse: DesSuccessResponse): List[JsValue] = {
  desSuccessResponse.taxYears.getOrElse(Nil).map(
    taxYear => Json.toJson(keys.flatMap(key => pickOneValue(key, taxYear)).toMap))
}

val json = readJson("/200-success-matched-two-years.json").as[DesSuccessResponse]


pickAll(List("surname", "nationalInsuranceNumber"), json)

val values = List(Json.toJson(Map(
  "surname" -> "Surname",
  "nationalInsuranceNumber" -> "AB123456C"
)))