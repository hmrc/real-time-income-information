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

  val invalidCorrelationIdJson = readJson("conf/resources/400-invalid-correlation-id.json")
  val invalidPayload = readJson("conf/resources/400-invalid-payload.json")
  val invalidDateRange = readJson("conf/resources/400-invalid-date-range.json")
  val invalidDatesEqual = readJson("conf/resources/400-invalid-dates-equal.json")
  val noDataFoundNinoJson = readJson("conf/resources/404-no-data-nino.json") //?
  val notFoundNinoJson = readJson("conf/resources/404-not-found-nino.json")
  val serverErrorJson = readJson("conf/resources/500-server-error.json")
  val serviceUnavailableJson = readJson("conf/resources/503-service-unavailable.json")
  val successMatchOneYear = readJson("conf/resources/200-success-matched-one-year.json")
  val successMatchTwoYear = readJson("conf/resources/200-success-matched-two-years.json")
  val successsNoMatch = readJson("conf/resources/200-success-no-match.json")
  val exampleDwpRequest = readJson("conf/resources/example-dwp-request.json")
  val exampleDwpRequestInvalidNino = readJson("conf/resources/example-dwp-request-invalid-nino.json")
  val exampleDesRequest = readJson("conf/resources/example-des-request.json")
  val multipleErrors = readJson("conf/resources/400-multiple-errors.json")
  val exampleInvalidMatchingFieldDwpRequest = readJson("conf/resources/example-invalid-matching-field-dwp-request.json")
  val exampleInvalidFilterFieldDwpRequest = readJson("conf/resources/example-invalid-filter-field-dwp-request.json")
  val exampleInvalidDwpEmptyFieldsRequest = readJson("conf/resources/example-invalid-dwp-empty-fields-request.json")
  val exampleInvalidDwpDuplicateFields = readJson("conf/resources/example-invalid-dwp-duplicate-fields.json")
  val exampleInvalidDwpEmptyStringField = readJson("conf/resources/example-invalid-dwp-empty-string-fields-request.json")


  private def readJson(path: String) = {
    Json.parse(Source.fromFile(path).getLines().mkString)
  }
}
