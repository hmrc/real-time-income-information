
import com.eclipsesource.schema.{SchemaType, SchemaValidator}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, __}

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





case class RequestDetails(
                           nino: String,
                           serviceName: String,
                           fromDate: String,
                           toDate: String,
                           surname: String,
                           firstName: Option[String],
                           middleName: Option[String],
                           gender: Option[String],
                           initials: Option[String],
                           dateOfBirth: Option[String],
                           filterFields: List[String]
                         )

object RequestDetails {
  implicit val formats = Json.format[RequestDetails]
}


val exampleInvalidMatchingFieldDwpRequest = readJson("/example-invalid-matching-field-dwp-request.json")

//exampleInvalidMatchingFieldDwpRequest.as[RequestDetails]


def schemaValidationHandler(jsonToValidate: JsValue): Either[JsError, JsSuccess[JsValue]] = {

  val validator = new SchemaValidator()

  val schema: JsValue = {
    val resource = getClass.getResourceAsStream("/schemas/real-time-income-information-post-schema.json")
    Json.parse(Source.fromInputStream(resource).mkString)
  }

  if (validator.validate(Json.fromJson[SchemaType](schema).get)(jsonToValidate).isSuccess)
    Right(JsSuccess(jsonToValidate))
  else
    Left(JsError("Does not validate against any schema"))
}

schemaValidationHandler(exampleInvalidMatchingFieldDwpRequest)
