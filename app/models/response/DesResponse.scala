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

package models.response

import play.api.libs.json._

sealed trait DesResponse

case class DesSuccessResponse(matchPattern: Int, taxYears: Option[List[JsValue]]) extends DesResponse
case class DesFilteredSuccessResponse(matchPattern: Int, taxYears: List[JsValue]) extends DesResponse
case class DesSingleFailureResponse(code: String, reason: String) extends DesResponse
case class DesMultipleFailureResponse(failures: List[DesSingleFailureResponse]) extends DesResponse
case class DesUnexpectedResponse(code: String = "INTERNAL_SERVER_ERROR", reason: String = "Internal Server Error") extends DesResponse

object DesResponse {
  implicit val desSuccessFormats: Format[DesSuccessResponse] = Json.format[DesSuccessResponse]
  implicit val desFilteredSuccessFormats: Format[DesFilteredSuccessResponse] = Json.format[DesFilteredSuccessResponse]
  implicit val desUnexpectedFormats: Format[DesUnexpectedResponse] = Json.format[DesUnexpectedResponse]
  implicit val desSingleFailureFormats: Format[DesSingleFailureResponse] = Json.format[DesSingleFailureResponse]
  implicit val desMultipleFailureFormats: Format[DesMultipleFailureResponse] = Json.format[DesMultipleFailureResponse]
}
