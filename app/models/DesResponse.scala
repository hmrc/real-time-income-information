/*
 * Copyright 2020 HM Revenue & Customs
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
  implicit val desSuccessFormats: Format[DesSuccessResponse]                 = Json.format[DesSuccessResponse]
  implicit val desFilteredSuccessFormats: Format[DesFilteredSuccessResponse] = Json.format[DesFilteredSuccessResponse]
  implicit val desUnexpectedFormats: Format[DesUnexpectedResponse]           = Json.format[DesUnexpectedResponse]
  implicit val desSingleFailureFormats: Format[DesSingleFailureResponse]     = Json.format[DesSingleFailureResponse]
  implicit val desMultipleFailureFormats: Format[DesMultipleFailureResponse] = Json.format[DesMultipleFailureResponse]
  implicit val desNoResponseWrites: Writes[DesNoResponse]                    = Json.writes[DesNoResponse]

  implicit val desErrorResponseRead: Reads[DesErrorResponse] = new Reads[DesErrorResponse] {

    override def reads(json: JsValue): JsResult[DesErrorResponse] =
      if ((json \ "failures").asOpt[JsArray].isDefined)
        desMultipleFailureFormats.reads(json)
      else
        desSingleFailureFormats.reads(json)

  }

}
