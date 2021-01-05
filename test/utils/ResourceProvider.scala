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

package utils

import play.api.libs.json.{JsValue, Json}

import scala.io.Source

trait ResourceProvider {

  private val fakeTestNino = "QQ123456A"

  val invalidCorrelationIdJson: JsValue        = readJson("/400-invalid-correlation-id.json")
  val invalidPayload: JsValue                  = readJson("/400-invalid-payload.json")
  val invalidDateRange: JsValue                = readJson("/400-invalid-date-range.json")
  val invalidDatesEqual: JsValue               = readJson("/400-invalid-dates-equal.json")
  val noDataFoundNinoJson: JsValue             = readJson("/404-no-data-nino.json")
  val notFoundNinoJson: JsValue                = readJson("/404-not-found-nino.json")
  val serverErrorJson: JsValue                 = readJson("/500-server-error.json")
  val serviceUnavailableJson: JsValue          = readJson("/503-service-unavailable.json")
  val successMatchOneYear: JsValue             = readJson("/200-success-matched-one-year.json")
  val successMatchTwoYear: JsValue             = readJson("/200-success-matched-two-years.json")
  val successNoMatch: JsValue                  = readJson("/200-success-no-match.json")
  val successNoMatchGreaterThanZero: JsValue   = readJson("/200-success-no-match-greater-than-zero.json")
  val exampleDwpRequest: JsValue               = readJson("/example-dwp-request.json")
  val exampleDesRequest: JsValue               = readJson("/example-des-request.json")
  val multipleErrors: JsValue                  = readJson("/400-multiple-errors.json")
  val exampleInvalidDateRangeRequest: JsValue  = readJson("/example-dwp-request-invalid-date-range.json")
  val exampleInvalidDatesEqualRequest: JsValue = readJson("/example-dwp-request-invalid-dates-equal.json")
  val exampleInvalidDatesNotDefined: JsValue   = readJson("/example-dwp-request-invalid-dates-not-defined.json")
  val exampleInvalidDateFormat: JsValue        = readJson("/example-dwp-request-invalid-date-format.json")

  def modifiedExampleDwpRequest(nino: String): JsValue =
    Json.parse(getResourceFileContent("/example-dwp-request.json").replace("QQ123456C", nino))

  def exampleDwpRequestInvalidNino(nino: String): JsValue =
    readJson("/example-dwp-request-invalid-nino.json", nino)

  def exampleInvalidMatchingFieldDwpRequest(nino: String): JsValue =
    readJson("/example-invalid-matching-field-dwp-request.json", nino)

  def exampleInvalidFilterFieldDwpRequest(nino: String): JsValue =
    readJson("/example-invalid-filter-field-dwp-request.json", nino)

  def exampleInvalidDwpEmptyFieldsRequest(nino: String): JsValue =
    readJson("/example-invalid-dwp-empty-fields-request.json", nino)

  def exampleInvalidDwpDuplicateFields(nino: String): JsValue =
    readJson("/example-invalid-dwp-duplicate-fields.json", nino)

  def exampleInvalidDwpEmptyStringField(nino: String): JsValue =
    readJson("/example-invalid-dwp-empty-string-fields-request.json", nino)

  def getResourceFileContent(resourceFile: String): String = {
    val is = getClass.getResourceAsStream(resourceFile)
    Source.fromInputStream(is).mkString
  }

  private def readJson(path: String): JsValue =
    Json.parse(getResourceFileContent(path))

  private def readJson(path: String, nino: String): JsValue =
    Json.parse(getResourceFileContent(path).replace(fakeTestNino, nino))

}
