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

package connectors


import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import models._
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(httpClient: HttpClient, desConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  private def header(correlationID: String): HeaderCarrier = HeaderCarrier(extraHeaders = Seq(
    "Authorization" -> desConfig.authorization,
    "Environment" -> desConfig.environment,
    "CorrelationId" -> correlationID))

  implicit val desReponseReads: HttpReads[DesResponse] = new HttpReads[DesResponse] {
    override def read(method: String, url: String, httpResponse: HttpResponse): DesResponse = httpResponse.status match {
      case OK => parseDesResponse[DesSuccessResponse](httpResponse)
      case _  => parseDesResponse[DesErrorResponse](httpResponse)
    }
  }

  private def parseDesResponse[A <: DesResponse](httpResponse: HttpResponse)(implicit reads: Reads[A]): DesResponse = {
    val handleError: Seq[(JsPath, scala.Seq[JsonValidationError])] => DesUnexpectedResponse = errors => {
      val extractValidationErrors: Seq[(JsPath, scala.Seq[JsonValidationError])] => String = errors => {
        //$COVERAGE-OFF$
        errors.map {
          case (path, List(validationError: JsonValidationError, _*)) => s"$path: ${validationError.message}"
        }.mkString(", ").trim
      }
      logger.error(s"Not able to parse the response received from DES with error ${extractValidationErrors(errors)}")
      //$COVERAGE-ON$
      DesUnexpectedResponse()
    }

    httpResponse.json.validate[A].fold(
      invalid = handleError,
      valid = identity
    )
  }

  def retrieveCitizenIncome(nino: String, matchingRequest: DesMatchingRequest, correlationId: String): Future[DesResponse] = {
    val postUrl: String = s"${desConfig.hodUrl}/individuals/$nino/income"
    implicit val hc: HeaderCarrier = header(correlationId)
    httpClient.POST[DesMatchingRequest, DesResponse](postUrl, matchingRequest) recover {
      case e: GatewayTimeoutException =>
        //$COVERAGE-OFF$
        logger.error(s"GatewayTimeoutException occurred: ${e.message}")
        //$COVERAGE-ON$
        DesNoResponse()
      case e: BadGatewayException =>
        //$COVERAGE-OFF$
        logger.error(s"BadGatewayException occurred: ${e.message}")
        //$COVERAGE-ON$
        DesNoResponse()
    }
  }
}
