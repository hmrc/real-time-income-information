/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import controllers.actions.AuthAction
import models.*
import models.ErrorCode.*
import models.ErrorResponse.*
import play.api.libs.json.*
import play.api.libs.json.Json.toJson
import play.api.mvc.*
import services.RealTimeIncomeInformationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Constants.correlationIdRegex
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RealTimeIncomeInformationController @Inject() (
    rtiiService: RealTimeIncomeInformationService,
    auth: AuthAction,
    cc: ControllerComponents
)(implicit
    ec: ExecutionContext
) extends BackendController(cc) {

  def retrieveRTII(correlationId: String): Action[JsValue] = auth.async(parse.json) { implicit request =>
    if(correlationId.matches(correlationIdRegex)) {
      request.body.validate[RequestDetails] match {
        case JsSuccess(requestDetails, _) =>
          val validationErrors = rtiiService.validateRequestDetails(requestDetails)
          validationErrors.length match {
            case 0 =>
              rtiiService.retrieveCitizenIncome(correlationId, requestDetails).map {
                case Right(successResponse) => Ok(Json.toJson(successResponse))
                case Left(e) => errorToResponse(e)
              }
            case 1 => Future.successful(BadRequest(Json.toJson(validationErrors.head)))
            case _ => Future.successful(BadRequest(Json.toJson(MultipleFailureResponse(validationErrors))))
          }
        case JsError(errors) =>  
          Future.successful(BadRequest(toJson(invalidPayloadWithMsg("Invalid Payload. Invalid " + errors.map(_._1).mkString(", ").replace("/", "")))))
      }
    }
    else {
      Future.successful(BadRequest(toJson(ErrorResponse(INVALID_CORRELATION_ID, "Submission has not passed validation. Invalid header CorrelationId."))))
    }
  }

  private def errorToResponse(error: UpstreamErrorResponse): Result =
    error match {
      case e if e.statusCode == NOT_FOUND =>
        if(e.message.contains(NOT_FOUND_NINO.toString))
          NotFound(Json.toJson(ErrorResponse(NOT_FOUND_NINO, "The remote endpoint has indicated that the Nino cannot be found.")))
        else
          NotFound(Json.toJson(ErrorResponse(ErrorCode.NOT_FOUND, "The remote endpoint has indicated that there is no data for the Nino.")))
      case e if e.statusCode == SERVICE_UNAVAILABLE => ServiceUnavailable(Json.toJson(serviceUnavailableError))
      case e if e.statusCode == TOO_MANY_REQUESTS    => TooManyRequests
      case _ => InternalServerError(Json.toJson(ErrorResponse(SERVER_ERROR, "Internal server error")))
    }
}
