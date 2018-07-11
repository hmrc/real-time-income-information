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

import config.BaseConfig
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.ContentTypes.JSON
import play.api.libs.json.Writes
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Matchers.{any, eq => eqs}

import scala.concurrent.{ExecutionContext, Future}

class ServiceLocatorConnectorSpec  extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val serviceLocatorException = new RuntimeException

    val mockHttpClient = mock[HttpClient]
    val mockConfig = mock[BaseConfig]
    val mockEnv = mock[Environment]
    val appUrl = "http://real-time-income-information.service"
    val appName = "real-time-income-information"
    val serviceUrl = "https://SERVICE_LOCATOR"
    class TestConfig extends BaseConfig(mockEnv) {

      override def getString(key: String): String = key match {
        case "appUrl" => appUrl
        case "appName" => appName
      }
      override def baseUrl(serviceName: String): String = {
        serviceUrl
      }

      override protected def runModeConfiguration: Configuration = mock[Configuration]
    }
    val connector: ServiceLocatorConnector = new ServiceLocatorConnector(mockHttpClient, new TestConfig) {
      override val handlerOK: () => Unit = mock[() => Unit]
      override val handlerError: Throwable => Unit = mock[(Throwable) => Unit]
      override val metadata: Option[Map[String, String]] = Some(
        Map("third-party-api" -> "true"))
    }
  }

  "register" should {
    "register the JSON API Definition into the Service Locator" in new Setup {

      val registration =
        Registration(serviceName = "real-time-income-information",
          serviceUrl = "http://real-time-income-information.service",
          metadata = Some(Map("third-party-api" -> "true")))

      when(mockHttpClient.POST(
        eqs(s"$serviceUrl/registration"),
        eqs(registration),
        eqs(Seq(CONTENT_TYPE -> JSON))
      )(
        any[Writes[Registration]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]
      ))
        .thenReturn(Future.successful(HttpResponse(200)))

      connector.register.futureValue shouldBe true
      verify(mockHttpClient).POST(
        eqs("https://SERVICE_LOCATOR/registration"),
        eqs(registration),
        eqs(Seq(CONTENT_TYPE -> JSON))
      )(
        any[Writes[Registration]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]
      )
      verify(connector.handlerOK).apply()
      verify(connector.handlerError, never).apply(serviceLocatorException)
    }

    "fail registering in service locator" in new Setup {

      val registration =
        Registration(serviceName = "real-time-income-information",
          serviceUrl = "http://real-time-income-information.service",
          metadata = Some(Map("third-party-api" -> "true")))
      when(mockHttpClient.POST(
        eqs(s"$serviceUrl/registration"),
        eqs(registration),
        eqs(Seq(CONTENT_TYPE -> JSON))
      )(
        any[Writes[Registration]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(Future.failed(serviceLocatorException))

      connector.register().futureValue shouldBe false
      verify(mockHttpClient).POST(
        eqs("https://SERVICE_LOCATOR/registration"),
        eqs(registration),
        eqs(Seq(CONTENT_TYPE -> JSON))
      )(
        any[Writes[Registration]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]
      )
      verify(connector.handlerOK, never).apply()
      verify(connector.handlerError).apply(serviceLocatorException)
    }
  }
}
