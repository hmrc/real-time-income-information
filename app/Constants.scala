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

package app

import models.response.DesSingleFailureResponse

object Constants {

  val errorCodeNotFoundNino = "NOT_FOUND_NINO"
  val errorCodeNotFound = "NOT_FOUND"
  val errorCodeInvalidCorrelation = "INVALID_CORRELATION_ID"
  val errorCodeInvalidDateRange = "INVALID_DATE_RANGE"
  val errorCodeInvalidDatesEqual = "INVALID_DATES_EQUAL"
  val errorCodeInvalidPayload = "INVALID_PAYLOAD"
  val errorCodeServerError = "SERVER_ERROR"
  val errorCodeServiceUnavailable = "SERVICE_UNAVAILABLE"
  val responseInvalidDateRange = DesSingleFailureResponse(errorCodeInvalidDateRange, "The remote endpoint has indicated that the date range is invalid.")
  val responseInvalidDatesEqual = DesSingleFailureResponse(errorCodeInvalidDatesEqual, "The remote endpoint has indicated that the from and to dates are the same.")
  val responseInvalidCorrelationId = DesSingleFailureResponse(errorCodeInvalidCorrelation, "Submission has not passed validation. Invalid header CorrelationId.")
  val responseInvalidPayload = DesSingleFailureResponse(errorCodeInvalidPayload, "Submission has not passed validation. Invalid Payload.")
  val responseNotFound = DesSingleFailureResponse(errorCodeNotFound, "The remote endpoint has indicated that there is no data for the Nino.")

}
