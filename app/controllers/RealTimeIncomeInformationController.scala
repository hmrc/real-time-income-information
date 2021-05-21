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

package controllers

import com.google.inject.{Inject, Singleton}
import controllers.actions.{AuthAction, ValidateCorrelationId}
import models._
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.{AuditService, RealTimeIncomeInformationService, RequestDetailsService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Constants._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

@Singleton
class RealTimeIncomeInformationController @Inject() (
    rtiiService: RealTimeIncomeInformationService,
    auditService: AuditService,
    auth: AuthAction,
    validateCorrelationId: ValidateCorrelationId,
    requestDetailsService: RequestDetailsService,
    cc: ControllerComponents
)(implicit
    ec: ExecutionContext
) extends BackendController(cc) {

  private val logger: Logger = Logger(this.getClass)

  def preSchemaValidation(correlationId: String): Action[JsValue] =
    authenticateAndValidate(correlationId).async(parse.json) { implicit request =>
      (parseJson andThen validateDate andThen getResult)(request) {
        requestDetails =>
          auditService.rtiiAudit(correlationId, requestDetails)
          rtiiService.retrieveCitizenIncome(requestDetails, correlationId) map {
            case filteredResponse: DesFilteredSuccessResponse        => Ok(Json.toJson(filteredResponse))
            case noMatchResponse: DesSuccessResponse                 => Ok(Json.toJson(noMatchResponse))
            case singleFailureResponse: DesSingleFailureResponse     => failureResponseToResult(singleFailureResponse)
            case multipleFailureResponse: DesMultipleFailureResponse => BadRequest(Json.toJson(multipleFailureResponse))
            case noResponse: DesNoResponse                           => ServiceUnavailable(Json.toJson(noResponse))
            case unexpectedResponse: DesUnexpectedResponse           => InternalServerError(Json.toJson(unexpectedResponse))
          } recover {
            case NonFatal(_) =>
              ServiceUnavailable(Json.toJson(responseServiceUnavailable))
          }
      }
    }

  private val parseJson: Request[JsValue] => Either[DesSingleFailureResponse, RequestDetails] = { request =>
    Try(request.body.validate[RequestDetails]) match {
      case Success(JsSuccess(value, _)) => Right(value)
      case Success(JsError(fieldErrors)) => {
        val error = fieldErrors.map(_._1)
        Left(invalidPayloadWithMsg("Invalid Payload. Invalid " + error.mkString(", ").replace("/", "")))
      }
      case Failure(exception) => Left(invalidPayloadWithMsg(exception.getMessage))
    }
  }

  private val validateDate
      : Either[DesSingleFailureResponse, RequestDetails] => Either[DesSingleFailureResponse, RequestDetails] =
    _.flatMap(requestDetailsService.validateDates)

  private val getResult
      : Either[DesSingleFailureResponse, RequestDetails] => (RequestDetails => Future[Result]) => Future[Result] =
    either => func => either.fold(singleFailure => Future.successful(BadRequest(Json.toJson(singleFailure))), func)

  private def authenticateAndValidate(id: String): ActionBuilder[Request, AnyContent] =
    auth andThen validateCorrelationId(id)

  private def failureResponseToResult(response: DesSingleFailureResponse): Result =
    Map(
      errorCodeServerError        -> InternalServerError(Json.toJson(response)),
      errorCodeNotFoundNino       -> NotFound(Json.toJson(response)),
      errorCodeNotFound           -> NotFound(Json.toJson(response)),
      errorCodeServiceUnavailable -> ServiceUnavailable(Json.toJson(response)),
      errorCodeInvalidCorrelation -> BadRequest(Json.toJson(response)),
      errorCodeInvalidDateRange   -> BadRequest(Json.toJson(response)),
      errorCodeInvalidDatesEqual  -> BadRequest(Json.toJson(response)),
      errorCodeInvalidPayload     -> BadRequest(Json.toJson(response))
    ).withDefault { _ =>
      //$COVERAGE-OFF$
      logger.error(s"Error from DES does not match schema: $response")
      //$COVERAGE-ON$
      InternalServerError(Json.toJson(response))
    }(response.code)

}
