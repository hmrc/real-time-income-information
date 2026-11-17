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

import models.ErrorCode.*
import play.api.libs.json.Json
import testUtils.BaseSpec

class ErrorResponseSpec extends BaseSpec {
  "ErrorResponse formats" must {
    "correctly read a single error ErrorResponse" in {
      noDataFoundNinoJson.as[ErrorResponse] mustBe ErrorResponse(
        NOT_FOUND,
        "The remote endpoint has indicated that there is no data for the Nino."
      )
    }
    "read a json with invalid ErrorCode as SERVER_ERROR" in {
      val errorResponse1 = Json.obj(
          "code" -> "invalid",
          "reason" -> "Anything"
      )
      val errorResponse2 = Json.obj(
          "code" -> true,
          "reason" -> "Something"
      )
      errorResponse1.as[ErrorResponse] mustBe ErrorResponse(SERVER_ERROR, "Anything")
      errorResponse2.as[ErrorResponse] mustBe ErrorResponse(SERVER_ERROR, "Something")
    }
    "correctly read multiple errors" in {
      multipleErrors.as[MultipleFailureResponse] mustBe MultipleFailureResponse(
        List(
          ErrorResponse(INVALID_PAYLOAD, "Submission has not passed validation. Invalid parameter nino."),
          ErrorResponse(INVALID_PAYLOAD, "Submission has not passed validation. Invalid Payload.")
        )
      )
    }
  }
}
