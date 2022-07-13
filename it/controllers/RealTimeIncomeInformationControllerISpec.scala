package controllers

import com.codahale.metrics.SharedMetricRegistries
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status => statusResult, _}
import test_utils.{IntegrationBaseSpec, WireMockHelper}
import uk.gov.hmrc.domain.Generator
import utils.Constants.responseServiceUnavailable

import java.util.UUID

class RealTimeIncomeInformationControllerISpec extends IntegrationBaseSpec with GuiceOneAppPerSuite with WireMockHelper with ScalaFutures {

  case class Enrolment (key: String, value: String = "")
  case class AuthBody (clientId: String = "localBearer", allEnrolments: Set[Enrolment], ttl: Int = 2000)

  implicit val enrolmentFormat = Json.format[Enrolment]
  implicit val authBodyFormat = Json.format[AuthBody]

  val apiAccessScope = "write:real-time-income-information"
  val filterFullAccessScope = "filter:real-time-income-information-full"
  val filterSgAccessScope = "filter:real-time-income-information-sg"

  def authBody(scope: String): String = {
    val body = AuthBody(allEnrolments = Set(Enrolment(apiAccessScope), Enrolment(scope)))
    Json.toJson(body).toString.stripMargin
  }

  override def fakeApplication() = GuiceApplicationBuilder().configure(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.des-hod.host" -> "127.0.0.1",
    "microservice.services.des-hod.port" -> server.port(),
    "metrics.jvm" -> false,
    "api.serviceName" -> Seq("serviceName")
  ).build()
  SharedMetricRegistries.clear()

  val generatedNino: String = new Generator().nextNino.nino
  val correlationId: String = UUID.randomUUID().toString

  "preSchemaValidation" should {
    "ServiceUnavailable when endpoint returns errorCode" in {
      stubPostServer(ok(authBody(filterFullAccessScope)), "/auth/authorise")

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

        val expectedResponse = getResponse("dwp-response", generatedNino)
        val desResponse = fullDesResponse(generatedNino)

        stubPostServer(ok(authBody(filterFullAccessScope)) ,"/auth/authorise")
        stubPostServer(ok(desResponse.toString()), s"/individuals/$generatedNino/income")

        val requestDetails = dwpRequest(generatedNino)
        val request = FakeRequest("POST", s"/individuals/$correlationId/income").withJsonBody(requestDetails)
        val result = route(fakeApplication(), request)

        result.map(statusResult) mustBe Some(OK)
        val resultValue: JsValue = result.map(x => await(jsonBodyOf(x))).get
        resultValue mustBe expectedResponse
      }

      "Consumer has access to all some fields requested but not all" in {

        val fileName = "sg-extra-fields"

        val expectedResponse = getResponse(fileName, generatedNino)
        val desResponse = fullDesResponse(generatedNino)

        stubPostServer(ok(authBody(filterSgAccessScope)) ,"/auth/authorise")
        stubPostServer(ok(desResponse.toString()), s"/individuals/$generatedNino/income")

        val requestDetails = getRequest(fileName, generatedNino)
        val request = FakeRequest("POST", s"/individuals/$correlationId/income").withJsonBody(requestDetails)
        val result = route(fakeApplication(), request)

        result.map(statusResult) mustBe Some(OK)
        val resultValue: JsValue = result.map(x => await(jsonBodyOf(x))).get
        resultValue mustBe expectedResponse
      }

      "Consumer has no access due to having no valid scope" in {

        val fileName = "sg-extra-fields"
        val expectedResponse =
          """{
            |"code":"INVALID_PAYLOAD",
            |"reason":"requirement failed: Submission has not passed validation. Invalid filter-fields in payload."
            |}""".stripMargin

        val desResponse = fullDesResponse(generatedNino)

        stubPostServer(ok(authBody("filter:this-is-not-a-valid-scope")) ,"/auth/authorise")
        stubPostServer(ok(desResponse.toString()), s"/individuals/$generatedNino/income")

        val requestDetails = getRequest(fileName, generatedNino)
        val request = FakeRequest("POST", s"/individuals/$correlationId/income").withJsonBody(requestDetails)

        val result = route(fakeApplication(), request)

        val resultValue: JsValue = result.map(x => await(jsonBodyOf(x))).get
        resultValue mustBe Json.parse(expectedResponse)
      }
    }
  }
}
