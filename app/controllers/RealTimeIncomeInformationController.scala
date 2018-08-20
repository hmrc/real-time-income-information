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
import models.RequestDetails
import models.response._
import play.api.Logger
import play.api.data.validation.Constraint
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import services.RealTimeIncomeInformationService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.SchemaValidationHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import org.joda.time.LocalDate

@Singleton
class RealTimeIncomeInformationController @Inject()(val rtiiService: RealTimeIncomeInformationService) extends BaseController with SchemaValidationHandler {

  def retrieveCitizenIncome(correlationId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      if(preSchemaValidation(correlationId, request.body)) {
        schemaValidationHandler(request.body) match {
          case Right(JsSuccess(_, _)) => withJsonBody[RequestDetails] {
            body =>
              rtiiService.retrieveCitizenIncome(body, correlationId) map {
                case filteredResponse: DesFilteredSuccessResponse => Ok(Json.toJson(filteredResponse))
                case noMatchResponse: DesSuccessResponse => NotFound(Json.toJson(Constants.responseNotFound))
                case singleFailureResponse: DesSingleFailureResponse => failureResponseToResult(singleFailureResponse)
                case multipleFailureResponse: DesMultipleFailureResponse => BadRequest(Json.toJson(multipleFailureResponse))
                case unexpectedResponse: DesUnexpectedResponse => InternalServerError(Json.toJson(unexpectedResponse))
              }
          }
          case Left(JsError(_)) => Future.successful(BadRequest(Json.toJson(Constants.responseInvalidPayload)))
        }
      } else
        Future.successful(BadRequest(Json.toJson(Constants.responseInvalidCorrelationId)))
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

  private def preSchemaValidation(correlationId: String, requestBody: JsValue): Boolean = {

    val toDate = new LocalDate(requestBody.as[RequestDetails].toDate)
    val fromDate = new LocalDate(requestBody.as[RequestDetails].fromDate)

    val dateCheck = fromDate.isBefore(toDate)&&(!toDate.isEqual(fromDate))

    val correlationIdRegex = """^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$""".r

    (correlationId, dateCheck) match {
      case (correlationIdRegex(_*), true) => true
      case _ => false
    }
  }
}
