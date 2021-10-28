package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status => statusResult, _}
import test_utils.{IntegrationBaseSpec, WireMockHelper}
import uk.gov.hmrc.domain.{Generator, Nino}
import utils.Constants.responseServiceUnavailable

import java.util.UUID

class RealTimeIncomeInformationControllerISpec extends IntegrationBaseSpec with GuiceOneAppPerSuite with WireMockHelper {

  def authBody(scope: String): String = s"""{
              | "clientId": "localBearer",
              | "allEnrolments": [{"key": "$scope", "value": ""}],
              | "ttl": 2000
              |}""".stripMargin

  override def fakeApplication() = GuiceApplicationBuilder().configure(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.des-hod.host" -> "127.0.0.1",
    "microservice.services.des-hod.port" -> server.port(),
    "metrics.jvm" -> false
  ).build()

  val generatedNino: String = new Generator().nextNino.nino
  val correlationId: String = UUID.randomUUID().toString

  "preSchemaValidation" should {
    "ServiceUnavailable when endpoint returns errorCode" in {
      stubPostServer(ok(authBody("write:real-time-income-information")), "/auth/authorise")

      stubPostServer(serviceUnavailable(), s"/individuals/$generatedNino/income")
      val requestDetails = dwpRequest(generatedNino)

      val request = FakeRequest("POST", s"/individuals/$correlationId/income").withJsonBody(requestDetails)
      val result = route(fakeApplication(), request)
      val expected = Some(Json.toJson(responseServiceUnavailable))

      result.map(statusResult) mustBe Some(SERVICE_UNAVAILABLE)
      result.map(contentAsJson(_)) mustBe expected
    }
  }

  "processFilterFields" must {
    "return requestDetails with filtered fields" when {
      "Consumer has access to all fields" in {

        stubPostServer(ok(authBody("write:real-time-income-information")) ,"/auth/authorise")


        val desBody = getDesRequest("dwp-request", generatedNino)
        val desResponse = getResponse("dwp-response", generatedNino)

        stubPostServerWithBody(ok(desResponse.toString()), desBody, s"/individuals/$generatedNino/income")

        val requestDetails = dwpRequest(generatedNino)
        val request = FakeRequest("POST", s"/individuals/$correlationId/income").withJsonBody(requestDetails)
        val result = route(fakeApplication(), request)

        result.map(statusResult) mustBe Some(OK)
      }

      // Does not have access to requested and filters out
      // Has access but filters based on subset requested
      // Has no access to scope returns no data


    }
  }
}
