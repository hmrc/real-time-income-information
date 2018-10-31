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

package controllers

import app.Constants
import com.google.inject.{Inject, Singleton}
import config.RTIIAuthConnector
import models.RequestDetails
import models.response._
import org.joda.time.LocalDate
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import services.{AuditService, RealTimeIncomeInformationService}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, UnsupportedAuthProvider}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.SchemaValidationHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class RealTimeIncomeInformationController @Inject()(val rtiiService: RealTimeIncomeInformationService, val auditService: AuditService, override val authConnector: RTIIAuthConnector) extends BaseController with SchemaValidationHandler with AuthorisedFunctions {

  def preSchemaValidation(correlationId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authorised(AuthProviders(PrivilegedApplication)) {
        if (validateCorrelationId(correlationId)) {
          validateDates(request.body) match {
            case Right(_) => retrieveCitizenIncome(correlationId)
            case Left(failure: DesSingleFailureResponse) => Future.successful(BadRequest(Json.toJson(failure)))
          }
        } else {
          Future.successful(BadRequest(Json.toJson(Constants.responseInvalidCorrelationId)))
        }
      } recover {
        case _: UnsupportedAuthProvider => Forbidden(Json.toJson(Constants.responseNonPrivilegedApplication))
      }
  }

  private def retrieveCitizenIncome(correlationId: String)(implicit hc: HeaderCarrier, request: Request[JsValue]) = {
    schemaValidationHandler(request.body) match {
      case Left(JsError(_)) => Future.successful(BadRequest(Json.toJson(Constants.responseInvalidPayload)))
      case Right(JsSuccess(_, _)) => withJsonBody[RequestDetails] {
        body =>
          auditService.rtiiAudit(correlationId, body)
          rtiiService.retrieveCitizenIncome(body, correlationId) map {
            case filteredResponse: DesFilteredSuccessResponse => Ok(Json.toJson(filteredResponse))
            case noMatchResponse: DesSuccessResponse => Ok(Json.toJson(noMatchResponse))
            case singleFailureResponse: DesSingleFailureResponse => failureResponseToResult(singleFailureResponse)
            case multipleFailureResponse: DesMultipleFailureResponse => BadRequest(Json.toJson(multipleFailureResponse))
            case unexpectedResponse: DesUnexpectedResponse => InternalServerError(Json.toJson(unexpectedResponse))
          }
      }
    }
  }

  private def failureResponseToResult(r: DesSingleFailureResponse): Result = {
    val results = Map(
      Constants.errorCodeServerError -> InternalServerError(Json.toJson(r)),
      Constants.errorCodeNotFoundNino -> NotFound(Json.toJson(r)),
      Constants.errorCodeNotFound -> NotFound(Json.toJson(r)),
      Constants.errorCodeServiceUnavailable -> ServiceUnavailable(Json.toJson(r)),
      Constants.errorCodeInvalidCorrelation -> BadRequest(Json.toJson(r)),
      Constants.errorCodeInvalidDateRange -> BadRequest(Json.toJson(r)),
      Constants.errorCodeInvalidDatesEqual -> BadRequest(Json.toJson(r)),
      Constants.errorCodeInvalidPayload -> BadRequest(Json.toJson(r))
    )

    Try(results(r.code)) match {
      case Success(result) => result
      case Failure(_) => Logger.info(s"Error from DES does not match schema: $r")
        InternalServerError(Json.toJson(r))
    }
  }

  private def validateCorrelationId(correlationId: String): Boolean = {
    val correlationIdRegex = """^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$""".r

    correlationId match {
      case correlationIdRegex(_*) => true
      case _ => false
    }
  }

  private def parseAsDate(string: String): Option[LocalDate] = {

    Try(new LocalDate(string)) match {
      case Success(date) => Some(date)
      case Failure(_) => None
    }
  }

  private def validateDates(requestBody: JsValue): Either[DesSingleFailureResponse, Boolean] = {

    val requestDetails: Try[RequestDetails] = Try(requestBody.as[RequestDetails])

    if (requestDetails.isFailure)
      Left(Constants.responseInvalidPayload)
    else {
      val toDate = parseAsDate(requestBody.as[RequestDetails].toDate)
      val fromDate = parseAsDate(requestBody.as[RequestDetails].fromDate)
      
      (toDate, fromDate) match {
        case (Some(endDate), Some(startDate)) => {
          val dateRangeValid = startDate.isBefore(endDate)
          val datesEqual = endDate.isEqual(startDate)

          (dateRangeValid, datesEqual) match {
            case (true, false) => Right(true)
            case (false, false) => Left(Constants.responseInvalidDateRange)
            case (_, true) => Left(Constants.responseInvalidDatesEqual)
          }
        }
        case _ => Left(Constants.responseInvalidPayload)
      }
    }
  }
}