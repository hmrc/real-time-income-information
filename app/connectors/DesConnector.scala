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

package connectors

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import models.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import repositories.RTIICacheRepository
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.http.HttpReads.Implicits.*

import java.util.UUID
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

trait DesConnector {
  def retrieveCitizenIncome(
                             nino: String,
                             matchingRequest: DesMatchingRequest
                           )(implicit
                             hc: HeaderCarrier
                           ): Future[Either[UpstreamErrorResponse, RTIISuccessResponse]]
}

class CachingDesConnector @Inject() (
                                      @Named("default") underlying: DesConnector,
                                      cacheRepo: RTIICacheRepository
                                    )(implicit
                                      ec: ExecutionContext
                                    ) extends DesConnector {
  override def retrieveCitizenIncome(nino: String, matchingRequest: DesMatchingRequest)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, RTIISuccessResponse]] = {
    val id = nino + matchingRequest.hashCode()
    val dataKey = DataKey[RTIISuccessResponse]("desResponse")
    for{
      repoData <- cacheRepo.get[RTIISuccessResponse](id)(dataKey)
      rtiiData <- repoData match {
        case Some(data) => Future(Right[UpstreamErrorResponse, RTIISuccessResponse](data))
        case None => for {
          apiData <- underlying.retrieveCitizenIncome(nino, matchingRequest)
          _ = apiData match {
            case Right(data) => cacheRepo.put[RTIISuccessResponse](id)(dataKey, data)
            case _ => identity
          }
        } yield apiData
      }
    } yield rtiiData

  }

}

@Singleton
class DefaultDesConnector @Inject() (
                                      httpClient: HttpClientV2,
                                      appConfig: ApplicationConfig,
                                      httpClientResponse: HttpClientResponse
                                    )(implicit
                                      ec: ExecutionContext
                                    ) extends DesConnector {

  private def buildHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      "Environment" -> appConfig.environment,
      HeaderNames.xSessionId -> hc.sessionId.fold("-")(_.value),
      HeaderNames.xRequestId -> hc.requestId.fold("-")(_.value),
      HeaderNames.authorisation -> appConfig.authorization,
      "CorrelationId" -> UUID.randomUUID().toString
    )

  def retrieveCitizenIncome(
                             nino: String,
                             matchingRequest: DesMatchingRequest
                           )(implicit
                             hc: HeaderCarrier
                           ): Future[Either[UpstreamErrorResponse, RTIISuccessResponse]] = {
    httpClientResponse.read(
      httpClient
        .post(url"${appConfig.hodUrl}/individuals/$nino/income")
        .withBody(Json.toJson(matchingRequest))
        .setHeader(buildHeaders*)
        .transform(_.withRequestTimeout(Duration(appConfig.timeoutInSeconds, SECONDS)))
        .execute[Either[UpstreamErrorResponse, RTIISuccessResponse]]
    )
  }
}
