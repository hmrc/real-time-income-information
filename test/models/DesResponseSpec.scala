/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers._
import utils.BaseSpec
import utils.ResourceProvider

class DesResponseSpec extends BaseSpec with ResourceProvider {

  "DesErrorResponse reads" must {
    "formatting DesSingleError" in {
      val desErrorResponse: DesErrorResponse = noDataFoundNinoJson.as[DesErrorResponse]
      desErrorResponse mustBe DesSingleFailureResponse(
        "NOT_FOUND",
        "The remote endpoint has indicated that there is no data for the Nino."
      )
    }
    "formatting DesMultipleError" in {
      val desErrorResponse: DesErrorResponse = multipleErrors.as[DesErrorResponse]
      desErrorResponse mustBe DesMultipleFailureResponse(
        List(
          DesSingleFailureResponse("INVALID_NINO", "Submission has not passed validation. Invalid parameter nino."),
          DesSingleFailureResponse("INVALID_PAYLOAD", "Submission has not passed validation. Invalid Payload.")
        )
      )
    }
  }
}
