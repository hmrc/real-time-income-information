/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import utils.{BaseSpec, Constants}

class RequestDetailsServiceSpec extends BaseSpec with GuiceOneAppPerSuite with Injecting {

  val SUT = inject[RequestDetailsService]

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
  }

  def createRequestDetails(fromDate: String, toDate: String) =
    RequestDetails(generateNino, "serviceName", fromDate, toDate, "surname", None, None, None, None, None, List("surname"))
}
