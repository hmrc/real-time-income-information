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

package services

import models.{DesSingleFailureResponse, RequestDetails}
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import utils.{BaseSpec, Constants}

class RequestDetailsServiceSpec extends BaseSpec with GuiceOneAppPerSuite with Injecting {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "api.serviceName" -> Seq("serviceName")
    ).build()

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

  "processServiceNames" must {
    "return Right of RequestDetails when serviceName is contained in appConfig list" in {
      val requestDetails = RequestDetails(generateNino, "serviceName", "2018-12-31", "2019-12-31", "Surname",
        None, None, None, None, None, List("surname"))

      SUT.processServiceName(requestDetails) mustBe Right(requestDetails)
    }

    "return Left of DesSingleFailureResponse when serviceName doesn't match config value" in {
      val requestDetails = RequestDetails(generateNino, "MadeUpService", "2018-12-31", "2019-12-31", "Surname",
        None, None, None, None, None, List("surname"))

      SUT.processServiceName(requestDetails) mustBe Left(DesSingleFailureResponse("INVALID_PAYLOAD",
        "requirement failed: Submission has not passed validation. Invalid serviceName in payload"))
    }
  }

  def createRequestDetails(fromDate: String, toDate: String) =
    RequestDetails(generateNino, "serviceName", fromDate, toDate, "surname", None, None, None, None, None, List("surname"))
}
