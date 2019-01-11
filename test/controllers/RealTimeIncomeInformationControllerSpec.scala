/*
 * Copyright 2019 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.http.Fault
import config.RTIIAuthConnector
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import services.{AuditService, RealTimeIncomeInformationService}
import test.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class RealTimeIncomeInformationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures with WireMockHelper with BaseSpec with IntegrationPatience {

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  protected lazy val service: RealTimeIncomeInformationService = injector.instanceOf[RealTimeIncomeInformationService]
  protected lazy val auditService: AuditService = injector.instanceOf[AuditService]
  protected lazy val rtiiAuthConnector: RTIIAuthConnector = injector.instanceOf[RTIIAuthConnector]
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 200
      }

      "the service returns a successful no match with a match pattern of 0" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok().withBody(successsNoMatch.toString)
            )
        )

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 200
      }

      "the service returns a successful no match with match pattern greater than 0" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok().withBody(successsNoMatchGreaterThanZero.toString)
            )
        )

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 400

      }

      "the correlationId is invalid" in {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(invalidCorrelationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the toDate is before fromDate" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDateRangeRequest))

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the toDate is equal to fromDate" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDatesEqualRequest))

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "a date is in the wrong format" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDateFormat))

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 400

      }
      "either fromDate or toDate is not defined in the request" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDatesNotDefined))

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 400
      }

      "the nino is invalid" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequestInvalidNino))

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 400
      }
    }

    "Return 403 (FORBIDDEN)" when {
      "A non privileged application attempts to call the endpoint" in {

          val fakeRequest = FakeRequest(method = "POST", uri = "",
            headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

          server.stubFor(
            post(urlEqualTo(s"/individuals/$nino/income"))
              .willReturn(
                ok(successMatchOneYear.toString())
              )
          )

          server.stubFor(
            post(urlEqualTo("/auth/authorise"))
              .willReturn(
                unauthorized().withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAuthProvider\"")
              )
          )

          val sut = createSUT(service, auditService, rtiiAuthConnector)
          val result = sut.preSchemaValidation(correlationId)(fakeRequest)
          status(result) mustBe 403
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 503
      }

      "DesConnector has thrown an Exception" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)
            )
        )

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
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

        server.stubFor(
          post(urlEqualTo("/auth/authorise"))
            .willReturn(
              ok("true")
            )
        )

        val sut = createSUT(service, auditService, rtiiAuthConnector)
        val result = sut.preSchemaValidation(correlationId)(fakeRequest)
        status(result) mustBe 500
      }
    }
  }

  def createSUT(rtiiService: RealTimeIncomeInformationService, auditService: AuditService, authConnector: RTIIAuthConnector) =
    new RealTimeIncomeInformationController(rtiiService, auditService, rtiiAuthConnector)

  private implicit val hc = HeaderCarrier()

  private val nino: String = "AB123456C"

  private val correlationId = UUID.randomUUID().toString
  private val invalidCorrelationId = "invalidCorrelationId"
}
