/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json._
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.mongo.cache.CacheIdType.SimpleCacheId
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesCache @Inject()(config: ApplicationConfig, mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
  extends MongoCacheRepository(
    mongoComponent = mongoComponent,
    collectionName = "des-cache",
    ttl = config.cacheExpireAfter,
    timestampSupport = new CurrentTimestampSupport(),
    cacheIdType = SimpleCacheId
  )

@Singleton
class DesConnector @Inject()(
                              httpClient: HttpClientV2,
                              desConfig: ApplicationConfig,
                              mongoCache: DesCache
)(implicit
  ec: ExecutionContext
) {

  private val logger: Logger = Logger(this.getClass)
  implicit val desReponseReads: HttpReads[DesResponse] = new HttpReads[DesResponse] {

    override def read(method: String, url: String, httpResponse: HttpResponse): DesResponse = {
      httpResponse.status match {
        case OK => parseDesResponse[DesSuccessResponse](httpResponse)
        case _  => parseDesResponse[DesErrorResponse](httpResponse)
      }
    }

  }

  private def parseDesResponse[A <: DesResponse](httpResponse: HttpResponse)(implicit
      reads: Reads[A]
  ): DesResponse = {
    val handleError: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])] => DesUnexpectedResponse = errors => {
      val extractValidationErrors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])] => String = errors => {
        //$COVERAGE-OFF$
        errors
          .map {
            error =>
              error._2.map(
                jsonValError => s"${error._1}: ${jsonValError.message}"
              )
          }
          .mkString(", ")
          .trim
      }
      logger.error(s"Not able to parse the response received from DES with error ${extractValidationErrors(errors)}")
      //$COVERAGE-ON$
      DesUnexpectedResponse()
    }

    httpResponse.json
      .validate[A]
      .fold(
        invalid = handleError,
        valid = identity
      )
  }

  def cache[A](id: String)(body: => Future[A])(implicit ev: Format[A]): Future[A] = {
    val dataKey = DataKey[A]("desResponse")

    mongoCache.get[A](id)(dataKey).flatMap {
      case Some(v) => Future.successful(v)
      case None    => body.flatMap { r => mongoCache.put(id)(dataKey, r).map { _ => r } }
    }
  }

  def retrieveCitizenIncome(
      nino: String,
      matchingRequest: DesMatchingRequest,
      correlationId: String
  )(implicit hc: HeaderCarrier): Future[DesResponse] = {

    cache[DesResponse](nino + matchingRequest.hashCode()) {

      val postUrl: String = s"${desConfig.hodUrl}/individuals/$nino/income"

      httpClient.post(url"$postUrl").withBody(Json.toJson(matchingRequest))
        .setHeader(HeaderNames.authorisation -> desConfig.authorization)
        .setHeader(HeaderNames.xRequestId -> hc.requestId.fold("-")(_.value))
        .setHeader(HeaderNames.xSessionId -> hc.sessionId.fold("-")(_.value))
        .setHeader("Environment" -> desConfig.environment)
        .setHeader("CorrelationId" -> UUID.randomUUID().toString)
        .execute[DesResponse] recover {
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

}
