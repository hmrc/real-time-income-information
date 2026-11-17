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

import com.google.inject.Inject
import models.RTIISuccessResponse
import play.api.Logging
import play.api.http.Status.*
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HttpException, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class HttpClientResponse @Inject() ()(implicit ec: ExecutionContext) extends Logging {
  private val logErrorResponses: PartialFunction[Try[Either[UpstreamErrorResponse, RTIISuccessResponse]], Unit] = {
    case Success(Left(r))           => logger.error(s"Response of ${r.statusCode} returned", r)
    case Failure(ex: HttpException) => logger.error(s"HttpException thrown: ${ex.message}", ex)
  }

  private val recoverHttpException: PartialFunction[Throwable, Either[UpstreamErrorResponse, RTIISuccessResponse]] = {
    case e: GatewayTimeoutException =>
      Left(UpstreamErrorResponse(message = e.message, SERVICE_UNAVAILABLE))
    case e: BadGatewayException =>
      Left(UpstreamErrorResponse(message = e.message, INTERNAL_SERVER_ERROR))
    case e: HttpException =>
      Left(UpstreamErrorResponse(message = e.message, INTERNAL_SERVER_ERROR))
  }

  def read(
            response: Future[Either[UpstreamErrorResponse, RTIISuccessResponse]]
          ): Future[Either[UpstreamErrorResponse, RTIISuccessResponse]] =
    response andThen logErrorResponses recover recoverHttpException
}
