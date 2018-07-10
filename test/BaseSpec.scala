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

import play.api.libs.json.Json

import scala.io.Source

trait BaseSpec {

  val invalidNinoJson = readJson("conf/resources/400-invalid-nino.json")
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
  val exampleRequest = readJson("conf/resources/example-request.json")
  val multipleErrors = readJson("conf/resources/400-multiple-errors.json")

  private def readJson(path: String) = {
    Json.parse(Source.fromFile(path).getLines().mkString)
  }
}
