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
import models.{DesMatchingRequest, RequestDetails}
import models.response._
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton //TODO httpClient Library getting java.lang.NoSuchMethodError: play.api.libs.json.JsArray.value()Lscala/collection/Seq;
class DesConnector @Inject()(httpClient: HttpClient,
                             desConfig: ApplicationConfig) extends RawReads { //TODO extending raw reads?

  def desPathUrl(nino: String) = s"${desConfig.hodUrl}/individuals/$nino/income"

  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  def commonHeaderValues(correlationId: String) = Seq(
    "Authorization" -> desConfig.authorization,
    "Environment" -> desConfig.environment,
    "CorrelationId" -> correlationId)

  def header(correlationID: String): HeaderCarrier = HeaderCarrier(extraHeaders = commonHeaderValues(correlationID))

  def retrieveCitizenIncome(nino: String, matchingRequest: DesMatchingRequest, correlationId: String)(implicit hc: HeaderCarrier): Future[DesResponse] = {
    val postUrl = desPathUrl(nino)
    implicit val hc: HeaderCarrier = header(correlationId)
    httpClient.POST(postUrl, matchingRequest).flatMap {
    httpResponse =>
      httpResponse.status match {
        case Status.OK => Future.successful(parseDesResponse[DesSuccessResponse](httpResponse))
        case _ => Future.successful(parseDesResponse[DesSingleFailureResponse](httpResponse))
      }
    }
  }

  private def parseDesResponse[A <: DesResponse](res: HttpResponse)
                                        (implicit reads: Reads[A]): DesResponse = {
    Try(res.json.as[A]) match { //TODO validate instead of try
      case Success(data) =>
        data
      case Failure(er) =>
        if (res.status == 200 | res.status == 201) {
          Logger.error(s"Error from DES (parsing as DesResponse): ${er.getMessage}")
        }

        Try(res.json.as[DesSingleFailureResponse]) match {
          case Success(data) => Logger.info(s"DesSingleFailureResponse from DES: $data")
            data
          case Failure(_) => Try(res.json.as[DesMultipleFailureResponse]) match {
            case Success(multipleFailures) => Logger.info(s"DesMultipleFailureResponse from DES: $multipleFailures")
              multipleFailures
            case Failure(unexpected) =>
              Logger.error(s"Error from DES (unable to parse as DesFailureResponse): ${unexpected.getMessage}")
              DesUnexpectedResponse()
          }
        }
    }
  }
}
