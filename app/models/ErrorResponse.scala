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

import models.ErrorCode.*
import play.api.libs.json.{Json, OFormat}

case class ErrorResponse(code: ErrorCode, reason: String)

object ErrorResponse {
  implicit val errorResponseFormat: OFormat[ErrorResponse]                   = Json.format[ErrorResponse]
  
  val serviceUnavailableError = ErrorResponse(SERVICE_UNAVAILABLE, "Dependent systems are currently not responding.")
  
  def invalidPayloadWithMsg(msg: String) = ErrorResponse(INVALID_PAYLOAD, msg)
  
}
