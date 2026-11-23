/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

class RTIISuccessResponse(val matchPattern: Int, val taxYears: Option[List[JsValue]])
object RTIISuccessResponse {
  implicit val format: OFormat[RTIISuccessResponse]                 =
    ((JsPath \ "matchPattern").format[Int] and
      (JsPath \ "taxYears").formatNullable[List[JsValue]]) (RTIISuccessResponse.apply, rtii => (rtii.matchPattern, rtii.taxYears))
}