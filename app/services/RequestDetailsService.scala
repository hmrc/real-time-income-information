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

package services

import com.google.inject.Inject
import config.APIConfig
import models.{DesSingleFailureResponse, RequestDetails}
import org.joda.time.LocalDate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

class RequestDetailsService @Inject()(apiConfig: APIConfig,
                                      val authConnector: AuthConnector) extends AuthorisedFunctions {

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

  def processFilterFields(requestDetails: RequestDetails)(implicit headerCarrier: HeaderCarrier,
                                                          executionContext: ExecutionContext): Either[DesSingleFailureResponse, RequestDetails] = {

    val enrolmentsFuture = authorised.retrieve(Retrievals.allEnrolments) {
      scopes => {
        println(scopes)
        Future.successful(scopes.enrolments.map(_.key))
      }
    }
    val enrolments: Set[String] = Await.result(enrolmentsFuture, atMost = 30 seconds)
    println(s"\n\n\n\n TESTTTTT $enrolments TTTTTSET \n\n\n\n")
    Right(requestDetails)
  }

  private def parseAsDate(string: String): Option[LocalDate] =
    Try(new LocalDate(string)).toOption
}
