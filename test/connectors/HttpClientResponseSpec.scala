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

import models.RTIISuccessResponse
import org.scalatest.RecoverMethods
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.*
import play.api.{Logger, Logging}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class HttpClientResponseSpec
  extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with RecoverMethods
    with Logging
    with LogCapturing {

  private lazy val httpClientResponseUsingMockLogger: HttpClientResponse = new HttpClientResponse with Logging {}
  private val dummyContent                                               = "error message"
  def getLogger: Logger                                                  = logger
  "read" must {
    behave like clientResponseLogger(
      httpClientResponseUsingMockLogger.read,
      infoLevel = Set(),
      warnLevel = Set(),
      errorLevel = Set(UNAUTHORIZED, FORBIDDEN, BAD_REQUEST, GATEWAY_TIMEOUT, SERVICE_UNAVAILABLE)
    )
  }

  private def clientResponseLogger(
                                    block: Future[Either[UpstreamErrorResponse, RTIISuccessResponse]] => Future[Either[UpstreamErrorResponse, RTIISuccessResponse]],
                                    infoLevel: Set[Int],
                                    warnLevel: Set[Int],
                                    errorLevel: Set[Int]
                                  ): Unit = {

    def testLogLevel(httpResponseCode: Int, logLevel: String): Unit =
      s"log message: $logLevel level only when response code is $httpResponseCode" in {
        withCaptureOfLoggingFrom(getLogger) { capturedLogs =>
          val response = Future(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
          whenReady(block(response)) { actual =>
            actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
            capturedLogs
              .filter(_.getLevel.toString == logLevel)
              .map(_.getMessage)
              .exists(_.contains(s"Response of $httpResponseCode returned")) mustBe true
          }
          ()
        }
      }

    infoLevel.foreach(testLogLevel(_, "INFO"))
    warnLevel.foreach(testLogLevel(_, "WARN"))
    errorLevel.foreach(testLogLevel(_, "ERROR"))

    "log message: ERROR level when future failed with HttpException & report as BAD GATEWAY" in {
      withCaptureOfLoggingFrom(getLogger) { capturedLogs =>
        val response = Future.failed(new HttpException(dummyContent, GATEWAY_TIMEOUT))
        whenReady(block(response)) { actual =>
          actual mustBe Left(
            UpstreamErrorResponse(message = dummyContent, statusCode = INTERNAL_SERVER_ERROR)
          )
          capturedLogs.exists(_.getMessage.contains(s"Exception thrown: $dummyContent")) mustBe true
        }
        ()
      }
    }

    "log nothing at all when future failed with non-HTTPException - let the error handler log these" in {
      withCaptureOfLoggingFrom(getLogger) { capturedLogs =>
        recoverToSucceededIf[RuntimeException](
          block(Future.failed[Either[UpstreamErrorResponse, RTIISuccessResponse]](new RuntimeException(dummyContent)))
        )
        capturedLogs.isEmpty mustBe true
        ()
      }
    }
  }
}
