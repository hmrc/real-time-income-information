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

package connectors


import com.google.inject.{Inject, Singleton}
import config.DesConfig
import models.MatchingDetails
import models.response.{DesFailureResponse, DesResponse, DesSuccessResponse, DesUnexpectedResponse}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class DesConnector @Inject()(httpClient: HttpClient,
                             desConfig: DesConfig) extends RawReads {

  def desPathUrl(nino: Nino) = s"${desConfig.baseURL}/individuals/$nino/income"

  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  def commonHeaderValues = Seq(
    "Environment" -> desConfig.environment,
    "Authorization" -> desConfig.authorization)

  def header: HeaderCarrier = HeaderCarrier(extraHeaders = commonHeaderValues)

  def retrieveCitizenIncome(nino: Nino, matchingFields: MatchingDetails)(implicit hc: HeaderCarrier): Future[DesResponse] = {
    val postUrl = desPathUrl(nino)
    httpClient.POST(postUrl, matchingFields).flatMap {
    httpResponse =>
      httpResponse.status match {
        case Status.OK => Future.successful(parseDesResponse[DesSuccessResponse](httpResponse))
        case _ => Future.successful(parseDesResponse[DesFailureResponse](httpResponse))
      }
    }
  }

  private def parseDesResponse[A <: DesResponse](res: HttpResponse)
                                        (implicit reads:Reads[A]): DesResponse = {
    Try(res.json.as[A]) match {
      case Success(data) =>
        data
      case Failure(er) =>
        if (res.status == 200 | res.status == 201) {
          Logger.error(s"Error from DES (parsing as DesResponse): ${er.getMessage}")
        }

        Try(res.json.as[DesFailureResponse]) match {
          case Success(data) => Logger.info(s"DesFailureResponse from DES: $data")
            data
          case Failure(ex) => Logger.error(s"Error from DES (parsing as DesFailureResponse): ${ex.getMessage}")
            DesUnexpectedResponse()
        }
    }
  }
}
