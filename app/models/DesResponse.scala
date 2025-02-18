/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.libs.json._

sealed trait DesResponse
sealed trait DesErrorResponse extends DesResponse

final case class DesSuccessResponse(matchPattern: Int, taxYears: Option[List[JsValue]]) extends DesResponse
final case class DesFilteredSuccessResponse(matchPattern: Int, taxYears: List[JsValue]) extends DesResponse
final case class DesSingleFailureResponse(code: String, reason: String) extends DesErrorResponse
final case class DesMultipleFailureResponse(failures: List[DesSingleFailureResponse]) extends DesErrorResponse

final case class DesUnexpectedResponse(code: String = "INTERNAL_SERVER_ERROR", reason: String = "Internal Server Error")
    extends DesErrorResponse

final case class DesNoResponse(code: String = "BAD_GATEWAY", reason: String = "Bad gateway calling des")
    extends DesErrorResponse

object DesResponse {

  private def read[A](json: JsValue, get: JsValue => JsResult[A]): JsResult[A] =
    (json \ "data").toOption.map(get).getOrElse(JsError("unable to read object"))

  private def write[A]($type: String, obj: A)(implicit ev: Writes[A]): JsValue =
    JsObject(Map("type" -> JsString($type), "data" -> ev.writes(obj)))

  implicit val desResponseFormats: Format[DesResponse] = new Format[DesResponse] {
    override def reads(json: JsValue): JsResult[DesResponse] =
      (json \ "type").toOption match {
        case Some(JsString("success"))           => read(json, desSuccessFormats.reads)
        case Some(JsString("filtered_success"))  => read(json, desFilteredSuccessFormats.reads)
        case Some(JsString("unexpected"))        => read(json, desUnexpectedFormats.reads)
        case Some(JsString("single_failure"))    => read(json, desSingleFailureFormats.reads)
        case Some(JsString("multiple_failures")) => read(json, desMultipleFailureFormats.reads)
        case Some(JsString("no_response"))       => read(json, desNoResponseFormats.reads)
        case Some(_)                             => JsError("unexpected $type")
        case None                                => JsError("$type not found")
      }

    override def writes(o: DesResponse): JsValue = o match {
      case r@DesSuccessResponse(_, _)         => write("success", r)
      case r@DesFilteredSuccessResponse(_, _) => write("filtered_success", r)
      case r@DesUnexpectedResponse(_, _)      => write("unexpected", r)
      case r@DesSingleFailureResponse(_, _)   => write("single_failure", r)
      case r@DesMultipleFailureResponse(_)    => write("multiple_failures", r)
      case r@DesNoResponse(_, _)              => write("no_response", r)
    }
  }

  implicit val desSuccessFormats: Format[DesSuccessResponse]                 = Json.format[DesSuccessResponse]
  implicit val desFilteredSuccessFormats: Format[DesFilteredSuccessResponse] = Json.format[DesFilteredSuccessResponse]
  implicit val desUnexpectedFormats: Format[DesUnexpectedResponse]           = Json.format[DesUnexpectedResponse]
  implicit val desSingleFailureFormats: Format[DesSingleFailureResponse]     = Json.format[DesSingleFailureResponse]
  implicit val desMultipleFailureFormats: Format[DesMultipleFailureResponse] = Json.format[DesMultipleFailureResponse]
  implicit val desNoResponseFormats: Format[DesNoResponse]                   = Json.format[DesNoResponse]

  implicit val desErrorResponseRead: Reads[DesErrorResponse] = new Reads[DesErrorResponse] {

    override def reads(json: JsValue): JsResult[DesErrorResponse] =
      if ((json \ "failures").asOpt[JsArray].isDefined)
        desMultipleFailureFormats.reads(json)
      else
        desSingleFailureFormats.reads(json)
  }
}
