/*
 * Copyright 2018 HM Revenue & Customs
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

package models.response

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait DesResponse

case class DesSuccessResponse(matchPattern: Int, taxYears: List[JsValue]) extends DesResponse
case class DesFilteredSuccessResponse(requestedFields: JsValue) extends DesResponse
case class DesSingleFailureResponse(code: String, reason: String) extends DesResponse
case class DesMultipleFailureResponse(failures: List[DesSingleFailureResponse]) extends DesResponse
case class DesUnexpectedResponse(code: String = "INTERNAL_SERVER_ERROR", reason: String = "Internal Server Error") extends DesResponse

object DesResponse {

  implicit val desSuccessFormats = Json.format[DesSuccessResponse]

  implicit val desFilteredSuccessFormats = Json.format[DesFilteredSuccessResponse]

  implicit val desUnexpectedFormats = Json.format[DesUnexpectedResponse]

  implicit val desMultipleFailureFormats: Format[DesMultipleFailureResponse] = Format(
    new Reads[DesMultipleFailureResponse] {
    override def reads(json: JsValue): JsResult[DesMultipleFailureResponse] = {
      JsSuccess(DesMultipleFailureResponse((json \ "failures").as[List[DesSingleFailureResponse]]))
    }},
    new Writes[DesMultipleFailureResponse] {
      override def writes(desMultipleFailure: DesMultipleFailureResponse): JsValue = {
        Json.obj("failures" -> desMultipleFailure.failures)
      }
    }
  )

  implicit val desSuccessReads: Reads[DesSuccessResponse] = (
    (JsPath \ "matchPattern").read[Int] and
    (JsPath \ "taxYears").read[List[JsValue]]
  )(DesSuccessResponse.apply _)

  implicit val desSuccessWrites: Writes[DesSuccessResponse] = (
    (JsPath \ "matchPattern").write[Int] and
      (JsPath \ "taxYears").write[List[JsValue]]
    )(unlift(DesSuccessResponse.unapply))

  implicit val desSingleFailureReads: Reads[DesSingleFailureResponse] = (
    (JsPath \ "code").read[String] and
      (JsPath \ "reason").read[String]
    )(DesSingleFailureResponse.apply _)

  implicit val desSingleFailureWrites: Writes[DesSingleFailureResponse] = (
    (JsPath \ "code").write[String] and
      (JsPath \ "reason").write[String]
    )(unlift(DesSingleFailureResponse.unapply))
}