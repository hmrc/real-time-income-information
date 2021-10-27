package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status => statusResult, _}
import test_utils.{IntegrationBaseSpec, WireMockHelper}
import uk.gov.hmrc.domain.Generator
import utils.Constants.responseServiceUnavailable

import java.util.UUID

class RealTimeIncomeInformationControllerISpec extends IntegrationBaseSpec with GuiceOneAppPerSuite with WireMockHelper {

  val body= """{
              | "clientId": "localBearer",
              | "enrolments": ["write:real-time-income-information"],
              | "ttl": 2000
              |}""".stripMargin

  override def fakeApplication() = GuiceApplicationBuilder().configure(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.des-hod.host" -> "127.0.0.1",
    "microservice.services.des-hod.port" -> server.port(),
    "metrics.jvm" -> false
  ).build()

  override def beforeEach() = {
    super.beforeEach()
    stubPostServer(ok(body), "/auth/authorise")
  }

  val generatedNino = new Generator().nextNino
  val correlationId = UUID.randomUUID().toString

  "preSchemaValidation" should {
    "ServiceUnavailable when endpoint returns errorCode" in {

      stubPostServer(serviceUnavailable(), s"/individuals/$generatedNino/income")
      val requestDetails = exampleDwpRequest(generatedNino.nino)

      val request = FakeRequest("POST", s"/individuals/$correlationId/income").withJsonBody(requestDetails)
      val result = route(fakeApplication(), request)
      val expected = Some(Json.toJson(responseServiceUnavailable))

      result.map(statusResult) mustBe Some(SERVICE_UNAVAILABLE)
      result.map(contentAsJson(_)) mustBe expected
    }
  }

  "processFilterFields" must {
    "return requestDetails with filtered fields" when {
      "enrolment filters fields" in {

        val requestDetails = exampleDwpRequest(generatedNino.nino)
        val request = FakeRequest("POST", s"/individuals/$correlationId/income").withJsonBody(requestDetails)
        val result = route(fakeApplication(), request)

        result.map(statusResult) mustBe Some(OK)
      }
    }
  }
}
