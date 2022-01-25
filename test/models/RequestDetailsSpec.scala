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

class RequestDetailsSpec extends BaseSpec {

  val nino: String = generateNino


  "return error message" when {
    List(
      ("the nino is invalid", exampleDwpRequestInvalidNino(nino),  "nino"),
      ("the filter fields array contains an empty string field", exampleInvalidDwpEmptyStringField(nino), "filter-fields"),
      ("the filter fields array contains duplicate fields", exampleInvalidDwpDuplicateFields(nino), "filter-fields"),
      ("the filter fields array is empty", exampleInvalidDwpEmptyFieldsRequest(nino), "filter-fields"),
      ("the request contains an unexpected filter field", exampleInvalidFilterFieldDwpRequest(nino), "filter-fields"),
      ("the request contains an invalid serviceName", exampleInvalidServiceName(nino), "serviceName"),
      ("the request contains an invalid todate", exampleInvalidToDateFormat(nino), "toDate"),
      ("the request contains an invalid fromdate", exampleInvalidFromDateFormat(nino), "fromDate"),
      ("the request contains an invalid surname", exampleInvalidSurname(nino), "surname"),
      ("the request contains an invalid middleName", exampleInvalidMiddleName(nino), "middle name"),
      ("the request contains an invalid firstName", exampleInvalidFirstName(nino), "first name"),
      ("the request contains an invalid gender", exampleInvalidGender(nino), "gender"),
      ("the request contains invalid initials", exampleInvalidInitials(nino), "initials"),
      ("the request contains invalid date of birth", exampleInvalidDob(nino), "date of birth")
    ).foreach {
      case (testName, json, error) =>
        testName in {
         intercept[IllegalArgumentException] {
         json.as[RequestDetails]
         }.getMessage mustBe s"requirement failed: Submission has not passed validation. Invalid $error in payload."
        }
      }
  }

  "toMatchingRequest" must {
    "create a DesMatchingRequest" in {
      val requestDetails = RequestDetails(
        nino = nino,
        serviceName = "searchlight",
        fromDate = "2016-12-31",
        toDate = "2017-12-31",
        surname = "Smith",
        firstName = Some("firstName"),
        middleName = Some("middleName"),
        gender = Some("M"),
        initials = Some("FMS"),
        dateOfBirth = Some("2050-03-04"),
        filterFields = List("surname", "nationalInsuranceNumber")
      )

      val transformedDESRequest: DesMatchingRequest = RequestDetails.toMatchingRequest(requestDetails)

      val expectedMatchingDESRequest = DesMatchingRequest(
        fromDate = "2016-12-31",
        toDate = "2017-12-31",
        surname = "Smith",
        firstName = Some("firstName"),
        middleName = Some("middleName"),
        gender = Some("M"),
        initials = Some("FMS"),
        dateOfBirth = Some("2050-03-04")
      )

      transformedDESRequest mustBe expectedMatchingDESRequest
    }
  }
}
