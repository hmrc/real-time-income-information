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

package services

import models.{DesSingleFailureResponse, RequestDetails}
import org.joda.time.LocalDate
import play.api.libs.json.JsValue
import utils.Constants

import scala.util.{Failure, Success, Try}

class RequestDetailsService {

  def x(requestDetails: RequestDetails): Option[DesSingleFailureResponse] = {
    val toDate = parseAsDate(requestDetails.toDate)
    val fromDate = parseAsDate(requestDetails.fromDate)
    (toDate, fromDate) match {
      case (Some(endDate), Some(startDate)) =>
        val dateRangeValid = startDate.isBefore(endDate)
        val datesEqual = endDate.isEqual(startDate)
        (dateRangeValid, datesEqual) match {
          case (true, false) => None
          case (false, false) => Some(Constants.responseInvalidDateRange)
          case (_, true) => Some(Constants.responseInvalidDatesEqual)
        }
      case _ => Some(Constants.responseInvalidPayload)
    }
  }

  private def validateDates(requestBody: JsValue): Either[DesSingleFailureResponse, Boolean] = {
    val tryRequestDetails: Try[RequestDetails] = Try(requestBody.as[RequestDetails])
    //TODO refactor this
    if (tryRequestDetails.isFailure)
      Left(Constants.responseInvalidPayload) //TODO handle at controller
    else {
      val requestDetails: RequestDetails = tryRequestDetails.get
      val toDate = parseAsDate(requestDetails.toDate)
      val fromDate = parseAsDate(requestDetails.fromDate)

      (toDate, fromDate) match {
        case (Some(endDate), Some(startDate)) =>
          val dateRangeValid = startDate.isBefore(endDate)
          val datesEqual = endDate.isEqual(startDate)

          (dateRangeValid, datesEqual) match {
            case (true, false) => Right(true)
            case (false, false) => Left(Constants.responseInvalidDateRange)
            case (_, true) => Left(Constants.responseInvalidDatesEqual)
          }
        case _ => Left(Constants.responseInvalidPayload)
      }
    }
  }

  private def parseAsDate(dateString: String): Option[LocalDate] = Try(new LocalDate(dateString)).toOption

}
