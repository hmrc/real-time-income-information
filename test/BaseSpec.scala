/*
 * Copyright 2018 HM Revenue & Customs
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

package test

import play.api.libs.json.Json

import scala.io.Source

trait BaseSpec {

  val invalidCorrelationIdJson = readJson("/400-invalid-correlation-id.json")
  val invalidPayload = readJson("/400-invalid-payload.json")
  val invalidDateRange = readJson("/400-invalid-date-range.json")
  val invalidDatesEqual = readJson("/400-invalid-dates-equal.json")
  val noDataFoundNinoJson = readJson("/404-no-data-nino.json")
  val notFoundNinoJson = readJson("/404-not-found-nino.json")
  val serverErrorJson = readJson("/500-server-error.json")
  val serviceUnavailableJson = readJson("/503-service-unavailable.json")
  val successMatchOneYear = readJson("/200-success-matched-one-year.json")
  val successMatchTwoYear = readJson("/200-success-matched-two-years.json")
  val successsNoMatch = readJson("/200-success-no-match.json")
  val exampleDwpRequest = readJson("/example-dwp-request.json")
  val exampleDwpRequestInvalidNino = readJson("/example-dwp-request-invalid-nino.json")
  val exampleDesRequest = readJson("/example-des-request.json")
  val multipleErrors = readJson("/400-multiple-errors.json")
  val exampleInvalidDateRangeRequest = readJson("/example-dwp-request-invalid-date-range.json")
  val exampleInvalidDatesEqualRequest = readJson("/example-dwp-request-invalid-dates-equal.json")
  val exampleInvalidDatesNotDefined = readJson("/example-dwp-request-invalid-dates-not-defined.json")
  val exampleInvalidDateFormat = readJson("/example-dwp-request-invalid-date-format.json")
  val exampleInvalidMatchingFieldDwpRequest = readJson("/example-invalid-matching-field-dwp-request.json")
  val exampleInvalidFilterFieldDwpRequest = readJson("/example-invalid-filter-field-dwp-request.json")
  val exampleInvalidDwpEmptyFieldsRequest = readJson("/example-invalid-dwp-empty-fields-request.json")
  val exampleInvalidDwpDuplicateFields = readJson("/example-invalid-dwp-duplicate-fields.json")
  val exampleInvalidDwpEmptyStringField = readJson("/example-invalid-dwp-empty-string-fields-request.json")

  private def readJson(path: String) = {
    val resource = getClass.getResourceAsStream(path)
    Json.parse(Source.fromInputStream(resource).getLines().mkString)
  }

}
