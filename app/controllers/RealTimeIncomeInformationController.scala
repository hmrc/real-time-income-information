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

package controllers

import com.google.inject.{Inject, Singleton}
import controllers.actions.{AuthAction, ValidateCorrelationId}
import models._
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.{AuditService, RealTimeIncomeInformationService, RequestDetailsService, SchemaValidator}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.Constants._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class RealTimeIncomeInformationController @Inject()(rtiiService: RealTimeIncomeInformationService,
                                                    auditService: AuditService,
                                                    auth: AuthAction,
                                                    validateCorrelationId: ValidateCorrelationId,
                                                    requestDetailsService: RequestDetailsService,
                                                    schemaValidator: SchemaValidator,
                                                    cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) {

  val logger: Logger = Logger(this.getClass)

  def preSchemaValidation(correlationId: String): Action[JsValue] = authenticateAndValidate(correlationId).async(parse.json) {
    implicit request =>
      (parseJson andThen validateDate andThen validateAgainstSchema(request.body) andThen getResult)(request) {
       requestDetails =>
         auditService.rtiiAudit(correlationId, requestDetails)
         rtiiService.retrieveCitizenIncome(requestDetails, correlationId) map {
           case filteredResponse: DesFilteredSuccessResponse => Ok(Json.toJson(filteredResponse))
           case noMatchResponse: DesSuccessResponse => Ok(Json.toJson(noMatchResponse))
           case singleFailureResponse: DesSingleFailureResponse => failureResponseToResult(singleFailureResponse)
           case multipleFailureResponse: DesMultipleFailureResponse => BadRequest(Json.toJson(multipleFailureResponse))
           case unexpectedResponse: DesUnexpectedResponse => InternalServerError(Json.toJson(unexpectedResponse))
         } recover {
           case NonFatal(_) =>
             ServiceUnavailable(Json.toJson(responseServiceUnavailable))
         }
      }
  }

  private val parseJson: Request[JsValue] => Either[DesSingleFailureResponse, RequestDetails] =
    request => request.body.validate[RequestDetails].fold[Either[DesSingleFailureResponse, RequestDetails]](
      _ => Left(responseInvalidPayload),
      Right(_)
    )

  private val validateDate: Either[DesSingleFailureResponse, RequestDetails] => Either[DesSingleFailureResponse, RequestDetails] =
    _.fold[Either[DesSingleFailureResponse, RequestDetails]](Left(_), requestDetailsService.validateDates)

  private val getResult: Either[DesSingleFailureResponse, RequestDetails] => (RequestDetails => Future[Result]) => Future[Result] =
    either => func => either.fold(singleFailure => Future.successful(BadRequest(Json.toJson(singleFailure))), func(_))

  private def authenticateAndValidate(id: String): ActionBuilder[Request, AnyContent] =
    auth andThen validateCorrelationId(id)

  private def validateAgainstSchema(json: JsValue): Either[DesSingleFailureResponse, RequestDetails] => Either[DesSingleFailureResponse, RequestDetails] = {
    either => either.fold(Left(_), rd => if (schemaValidator.validate(json)) Right(rd) else Left(responseInvalidPayload))
  }

  private def failureResponseToResult(r: DesSingleFailureResponse): Result =
   Map(
      errorCodeServerError -> InternalServerError(Json.toJson(r)),
      errorCodeNotFoundNino -> NotFound(Json.toJson(r)),
      errorCodeNotFound -> NotFound(Json.toJson(r)),
      errorCodeServiceUnavailable -> ServiceUnavailable(Json.toJson(r)),
      errorCodeInvalidCorrelation -> BadRequest(Json.toJson(r)),
      errorCodeInvalidDateRange -> BadRequest(Json.toJson(r)),
      errorCodeInvalidDatesEqual -> BadRequest(Json.toJson(r)),
      errorCodeInvalidPayload -> BadRequest(Json.toJson(r))
    ).withDefaultValue{
      logger.error(s"Error from DES does not match schema: $r")
      InternalServerError(Json.toJson(r))
    }(r.code)
}
