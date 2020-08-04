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

package models

import play.api.libs.json.Json

case class RequestDetails(
    nino: String,
    serviceName: String,
    fromDate: String,
    toDate: String,
    surname: String,
    firstName: Option[String],
    middleName: Option[String],
    gender: Option[String],
    initials: Option[String],
    dateOfBirth: Option[String],
    filterFields: List[String]
)

object RequestDetails {
  implicit val formats = Json.format[RequestDetails]

  def toMatchingRequest(r: RequestDetails): DesMatchingRequest =
    DesMatchingRequest(r.fromDate, r.toDate, r.surname, r.firstName, r.middleName, r.gender, r.initials, r.dateOfBirth)

}
