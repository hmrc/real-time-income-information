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
import models.response.{DesFilteredSuccessResponse, DesMultipleFailureResponse, DesSingleFailureResponse, DesUnexpectedResponse}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import services.RealTimeIncomeInformationService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.SchemaValidationHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class RealTimeIncomeInformationController @Inject()(val rtiiService: RealTimeIncomeInformationService) extends BaseController with SchemaValidationHandler {

  def retrieveCitizenIncome(correlationId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      schemaValidationHandler(request.body) match {
        case Right(JsSuccess(requestBody, _)) => withJsonBody[RequestDetails] {
          body =>
            rtiiService.retrieveCitizenIncome (Nino (body.nino), body) map {
              case filteredResponse: DesFilteredSuccessResponse => Ok (Json.toJson (filteredResponse))
              case singleFailureResponse: DesSingleFailureResponse => failureResponseToResult (singleFailureResponse)
              case multipleFailureResponse: DesMultipleFailureResponse => BadRequest (Json.toJson(multipleFailureResponse))
              case unexpectedResponse: DesUnexpectedResponse => InternalServerError (Json.toJson(unexpectedResponse))
          }
        }
        case Left(JsError(_)) => Future.successful(BadRequest(Json.toJson(Constants.responseErrorCodeInvalidPayload)))
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
      Constants.errorCodeInvalidPayload -> BadRequest(Json.toJson(r)))

    Try(results(r.code)) match {
      case Success(result) => result
      case Failure(_) => Logger.info(s"Error from DES does not match schema: $r")
        InternalServerError(Json.toJson(r))
    }
  }

}
