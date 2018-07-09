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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock.{badRequest, ok, post, urlEqualTo, notFound, serverError, serviceUnavailable}
import connectors.DesConnector
import controllers.RealTimeIncomeInformationController
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import services.RealTimeIncomeInformationService
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

import scala.util.Random

class RealTimeIncomeInformationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures with WireMockHelper with BaseSpec with IntegrationPatience {

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  protected lazy val service: RealTimeIncomeInformationService = injector.instanceOf[RealTimeIncomeInformationService]
  protected lazy val controller: RealTimeIncomeInformationController = injector.instanceOf[RealTimeIncomeInformationController]

  "RealTimeIncomeInformationController" should {
    "Return 200" when {
      "the service returns a successfully filtered response" in  {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(nino)(fakeRequest)
        status(result) mustBe 200
      }
    }

    "Return 400" when {
      "the service returns a single error response" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              badRequest().withBody(invalidCorrelationIdJson.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(nino)(fakeRequest)
        status(result) mustBe 400

      }

      "the service returns multiple error responses" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              badRequest().withBody(multipleErrors.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(nino)(fakeRequest)
        status(result) mustBe 400

      }
    }
    "Return 404 (NOT_FOUND)" when {
      "The remote endpoint has indicated that there is no data for the Nino" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              notFound().withBody(notFoundNinoJson.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(nino)(fakeRequest)
        status(result) mustBe 404
      }
    }

    "Return 500 (SERVER_ERROR)" when {
      "DES is currently experiencing problems that require live service intervention." in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serverError().withBody(serverErrorJson.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(nino)(fakeRequest)
        status(result) mustBe 500
      }
    }

    "Return 503 (SERVICE_UNAVAILABLE)" when {
      "Dependent systems are currently not responding" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serviceUnavailable().withBody(serviceUnavailableJson.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(nino)(fakeRequest)
        status(result) mustBe 503
      }
    }

    "Return INTERNAL_SERVER_ERROR" when {
      "DES has given an unexpected response" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serverError().withBody("INTERNAL_SERVER_ERROR")
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(nino)(fakeRequest)
        status(result) mustBe 500
      }
    }
  }

  def createSUT(rtiiService: RealTimeIncomeInformationService) =
    new RealTimeIncomeInformationController(rtiiService)

  private implicit val hc = HeaderCarrier()

  private def randomNino: Nino = new Generator(new Random).nextNino
  private val nino: String = randomNino.nino

  private val correlationId = UUID.randomUUID()
}
