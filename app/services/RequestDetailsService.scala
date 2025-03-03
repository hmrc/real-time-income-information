/*
 * Copyright 2025 HM Revenue & Customs
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

import com.google.inject.Inject
import config.{APIConfig, ApiScope}
import controllers.actions.AuthenticatedRequest
import models.{DesSingleFailureResponse, RequestDetails}
import utils.Constants
import utils.Constants.invalidPayloadWithMsg

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

class RequestDetailsService @Inject()(apiConfig: APIConfig) {

  def validateDates(requestDetails: RequestDetails): Either[DesSingleFailureResponse, RequestDetails] = {
    val toDate   = parseAsDate(requestDetails.toDate)
    val fromDate = parseAsDate(requestDetails.fromDate)

    (toDate, fromDate) match {
      case (Some(endDate), Some(startDate)) =>
        val dateRangeValid = startDate.isBefore(endDate)
        val datesEqual     = endDate.isEqual(startDate)

        (dateRangeValid, datesEqual) match {
          case (true, false)  => Right(requestDetails)
          case (false, false) => Left(Constants.responseInvalidDateRange)
          case (_, true)      => Left(Constants.responseInvalidDatesEqual)
        }
      case _ => Left(Constants.responseInvalidPayload)
    }
  }

  def processFilterFields(requestDetails: RequestDetails)(implicit ar: AuthenticatedRequest[?]): Either[DesSingleFailureResponse, RequestDetails] = {
    val enrolments = ar.authDetails.enrolments.enrolments.map(_.key)

    val scopes: Set[ApiScope] = enrolments.flatMap(apiConfig.findScope)
    val accessibleFields: Set[String] = scopes.flatMap(_.getFieldNames())
    val usableFields = requestDetails.filterFields.filter(accessibleFields.contains)

    Try(requestDetails.copy(filterFields = usableFields)) match {
      case Success(filteredRequestDetails) => Right(filteredRequestDetails)
      case Failure(_) => Left(invalidPayloadWithMsg("requirement failed: Submission has not passed validation. Invalid filter-fields in payload."))
    }
  }

  def processServiceName(requestDetails: RequestDetails): Either[DesSingleFailureResponse, RequestDetails] =
    if (apiConfig.serviceNames.contains(requestDetails.serviceName)) Right(requestDetails)
    else Left(invalidPayloadWithMsg("requirement failed: Submission has not passed validation. Invalid serviceName in payload"))

  private def parseAsDate(string: String): Option[LocalDate] =
    Try(LocalDate.parse(string)).toOption
}
