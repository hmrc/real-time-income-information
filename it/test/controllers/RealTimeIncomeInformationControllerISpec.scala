/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.{OK, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.test.Helpers.{POST, contentAsJson, defaultAwaitTimeout, route}
import play.api.test.{FakeHeaders, FakeRequest}
import test_utils.{IntegrationBaseSpec, WiremockHelper}
import uk.gov.hmrc.domain.Generator
import utils.Constants.responseServiceUnavailable

import java.util.UUID

class RealTimeIncomeInformationControllerISpec extends IntegrationBaseSpec with GuiceOneAppPerSuite with WiremockHelper with ScalaFutures {

  case class Enrolment (key: String, value: String = "")
  case class AuthBody (clientId: String = "localBearer", allEnrolments: Set[Enrolment], ttl: Int = 2000)

  implicit val enrolmentFormat: OFormat[Enrolment] = Json.format[Enrolment]
  implicit val authBodyFormat: OFormat[AuthBody] = Json.format[AuthBody]

  val apiAccessScope = "write:real-time-income-information"
  val filterFullAccessScope = "filter:real-time-income-information-full"
  val filterSgAccessScope = "filter:real-time-income-information-sg"
  val filterCspAccessScope = "filter:real-time-income-information-csp"

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

  val generatedNino: String = new Generator().nextNino.nino
  val correlationId: String = UUID.randomUUID().toString

  "preSchemaValidation" should {
    "ServiceUnavailable when endpoint returns errorCode" in {
      stubPostServer(ok(authBody(filterFullAccessScope)), "/auth/authorise")
      stubPostServer(serviceUnavailable(), s"/individuals/$generatedNino/income")

      val requestDetails = dwpRequest(generatedNino)
      val request = FakeRequest(
        method = POST,
        uri = s"/individuals/$correlationId/income",
        headers = FakeHeaders(Seq(
          "Authorization" -> "Bearer bearer-token"
        )),
        body = requestDetails
      )

      val result = route(fakeApplication(), request)
      val expected = Some(Json.toJson(responseServiceUnavailable))

      eventually(timeout(Span(30, Seconds))) {
        result.map(status) mustBe Some(SERVICE_UNAVAILABLE)
        result.map(contentAsJson(_)) mustBe expected
      }
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
        val request = FakeRequest(
          method = POST,
          uri = s"/individuals/$correlationId/income",
          headers = FakeHeaders(Seq(
            "Authorization" -> "Bearer bearer-token"
          )),
          body = requestDetails
        )

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(OK)
        val resultValue: JsValue = result.map(x => await(jsonBodyOf(x))).get
        resultValue mustBe expectedResponse
      }

      "Consumer has access to all some fields requested but not all - sg" in {

        val fileName = "filtered-extra-fields"

        val expectedResponse = getResponse(fileName, generatedNino)
        val desResponse = fullDesResponse(generatedNino)

        stubPostServer(ok(authBody(filterSgAccessScope)) ,"/auth/authorise")
        stubPostServer(ok(desResponse.toString()), s"/individuals/$generatedNino/income")

        val requestDetails = getRequest(fileName, generatedNino)
        val request = FakeRequest(
          method = POST,
          uri = s"/individuals/$correlationId/income",
          headers = FakeHeaders(Seq(
            "Authorization" -> "Bearer bearer-token"
          )),
          body = requestDetails
        )

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(OK)
        val resultValue: JsValue = result.map(x => await(jsonBodyOf(x))).get
        resultValue mustBe expectedResponse
      }

      "Consumer has access to all some fields requested but not all - csp" in {

        val fileName = "filtered-extra-fields"

        val expectedResponse = getResponse(fileName, generatedNino)
        val desResponse = fullDesResponse(generatedNino)

        stubPostServer(ok(authBody(filterCspAccessScope)) ,"/auth/authorise")
        stubPostServer(ok(desResponse.toString()), s"/individuals/$generatedNino/income")

        val requestDetails = getRequest(fileName, generatedNino)
        val request = FakeRequest(
          method = POST,
          uri = s"/individuals/$correlationId/income",
          headers = FakeHeaders(Seq(
            "Authorization" -> "Bearer bearer-token"
          )),
          body = requestDetails
        )

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(OK)
        val resultValue: JsValue = result.map(x => await(jsonBodyOf(x))).get
        resultValue mustBe expectedResponse
      }

      "Consumer has no access due to having no valid scope" in {

        val fileName = "filtered-extra-fields"
        val expectedResponse =
          """{
            |"code":"INVALID_PAYLOAD",
            |"reason":"requirement failed: Submission has not passed validation. Invalid filter-fields in payload."
            |}""".stripMargin

        val desResponse = fullDesResponse(generatedNino)

        stubPostServer(ok(authBody("filter:this-is-not-a-valid-scope")) ,"/auth/authorise")
        stubPostServer(ok(desResponse.toString()), s"/individuals/$generatedNino/income")

        val requestDetails: JsValue = getRequest(fileName, generatedNino)
        val request = FakeRequest(
          method = POST,
          uri = s"/individuals/$correlationId/income",
          headers = FakeHeaders(Seq(
            "Authorization" -> "Bearer bearer-token"
          )),
          body = requestDetails
        )

        val result = route(fakeApplication(), request)

        val resultValue: JsValue = result.map(x => await(jsonBodyOf(x))).get
        resultValue mustBe Json.parse(expectedResponse)
      }
    }
  }
}
