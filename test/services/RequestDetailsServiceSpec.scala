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

package services

import models.RequestDetails
import utils.{BaseSpec, Constants}

class RequestDetailsServiceSpec extends BaseSpec {

  val SUT = new RequestDetailsService

  "validateDates" must {
    "return requestDetails" when {
      "date range is valid and dates are not equal" in {
        val requestDetails = createRequestDetails("2016-12-31", "2017-12-31")

        SUT.validateDates(requestDetails) mustBe Right(requestDetails)
      }
    }

    "return responseInvalidDateRange" when {
      "date range is invalid and dates are not equal" in {
        val requestDetails = createRequestDetails("2018-12-31", "2017-12-31")

        SUT.validateDates(requestDetails) mustBe Left(Constants.responseInvalidDateRange)
      }
    }

    "return responseInvalidDatesEqual" when {
      "date range is invalid and dates are equal" in {
        val requestDetails = createRequestDetails("2018-12-31", "2018-12-31")

        SUT.validateDates(requestDetails) mustBe Left(Constants.responseInvalidDatesEqual)
      }
    }

    "return responseInvalidPayload" when {
      "the dates are not parsable" in {
        val requestDetails = createRequestDetails("non parsable", "date")

        SUT.validateDates(requestDetails) mustBe Left(Constants.responseInvalidPayload)
      }
    }
  }

  def createRequestDetails(fromDate: String, toDate: String) =
    RequestDetails("", "", fromDate, toDate, "", None, None, None, None, None, Nil)

}
