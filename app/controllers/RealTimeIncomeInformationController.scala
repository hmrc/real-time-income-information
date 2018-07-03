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

import com.google.inject.{Inject, Singleton}
import connectors.DesConnector
import models.RequestDetails
import models.response.{DesFailureResponse, DesSuccessResponse, DesUnexpectedResponse}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RealTimeIncomeInformationController @Inject()(val desConnector: DesConnector) extends BaseController {

  def retrieveCitizenIncome(nino: String)= Action.async(parse.json) {
    implicit request =>
      withJsonBody[RequestDetails] { body =>
          desConnector.retrieveCitizenIncome (Nino(nino), body) map {
            case desSuccess: DesSuccessResponse => Ok(Json.toJson(desSuccess))
            case desFailure: DesFailureResponse => BadRequest(Json.toJson(desFailure))
            case _: DesUnexpectedResponse => InternalServerError
          }
      }
  }
}
