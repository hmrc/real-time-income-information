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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import services.RealTimeIncomeInformationService
import test.BaseSpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

import scala.util.Random

class RealTimeIncomeInformationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures with WireMockHelper with BaseSpec with IntegrationPatience {

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  protected lazy val service: RealTimeIncomeInformationService = injector.instanceOf[RealTimeIncomeInformationService]
  protected lazy val controller: RealTimeIncomeInformationController = injector.instanceOf[RealTimeIncomeInformationController]

  "RealTimeIncomeInformationController" should {
    "Return 200 provided a valid request" when {
      "the service returns a successfully filtered response" in  {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 200
      }
    }

    "Return 400" when {
      "the request contains an unexpected matching field" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidMatchingFieldDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the request contains an unexpected filter field" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidFilterFieldDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the filter fields array is empty" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDwpEmptyFieldsRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the filter fields array contains duplicate fields" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDwpDuplicateFields))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the filter fields array contains an empty string field" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDwpEmptyStringField))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the service returns a single error response" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              badRequest().withBody(invalidCorrelationIdJson.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400

      }

      "the service returns multiple error responses" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              badRequest().withBody(multipleErrors.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400

      }

      "the correlationId is invalid" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(invalidCorrelationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the toDate is before fromDate" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDateRangeRequest))

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the toDate is equal to fromDate" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDatesEqualRequest))

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the nino is invalid" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequestInvalidNino))

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 400
      }
    }

    "Return 404 (NOT_FOUND)" when {
      "The remote endpoint has indicated that there is no data for the Nino" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              notFound().withBody(notFoundNinoJson.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 404
      }

      "The remote endpoint has indicated a 200 no match" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok().withBody(successsNoMatch.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 404
      }
    }

    "Return 500 (SERVER_ERROR)" when {
      "DES is currently experiencing problems that require live service intervention." in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serverError().withBody(serverErrorJson.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 500
      }
    }

    "Return 503 (SERVICE_UNAVAILABLE)" when {
      "Dependent systems are currently not responding" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serviceUnavailable().withBody(serviceUnavailableJson.toString)
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 503
      }
    }

    "Return INTERNAL_SERVER_ERROR" when {
      "DES has given an unexpected response" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serverError().withBody("INTERNAL_SERVER_ERROR")
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 500
      }

      "DES has given a failure code and reason that do not match schema" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serverError().withBody("""{ "code": "error", "reason":"error"}""")
            )
        )

        val sut = createSUT(service)
        val result = sut.retrieveCitizenIncome(correlationId)(fakeRequest)
        status(result) mustBe 500
      }
    }
  }

  def createSUT(rtiiService: RealTimeIncomeInformationService) =
    new RealTimeIncomeInformationController(rtiiService)

  private implicit val hc = HeaderCarrier()

  private val nino: String = "AB123456C"

  private val correlationId = UUID.randomUUID().toString
  private val invalidCorrelationId = "invalidCorrelationId"
}
