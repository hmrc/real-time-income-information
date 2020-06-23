/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.stream.Materializer
import app.Constants
import models.response.{DesFilteredSuccessResponse, DesSingleFailureResponse, DesSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{FakeHeaders, FakeRequest, Injecting}
import play.api.test.Helpers._
import services.{AuditService, RealTimeIncomeInformationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import utils.FakeAuthConnector
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import test.BaseSpec

import scala.concurrent.Future

class RealTimeIncomeInformationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with MockitoSugar with BaseSpec with BeforeAndAfterEach {

  private val correlationId = UUID.randomUUID().toString
  val mockRtiiService: RealTimeIncomeInformationService = mock[RealTimeIncomeInformationService]
  val mockAuditService: AuditService = mock[AuditService]
  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[RealTimeIncomeInformationService].toInstance(mockRtiiService),
        bind[AuditService].toInstance(mockAuditService),
        bind[AuthConnector].toInstance(FakeAuthConnector))
      .build()
  }

  override def beforeEach() = {
    super.beforeEach()
    reset(mockRtiiService, mockAuditService)
  }

  implicit val mat: Materializer = app.materializer
  val controller: RealTimeIncomeInformationController = inject[RealTimeIncomeInformationController]

  "RealTimeIncomeInformationController" should {
    "Return OK provided a valid request" when {
      "the service returns a successfully filtered response" in  {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")),
          body = Json.toJson(exampleDwpRequest)
        )

        val values = Json.toJson(Map(
          "surname" -> "Surname",
          "nationalInsuranceNumber" -> "AB123456C"
        ))

        val expectedDesResponse = DesFilteredSuccessResponse(63, List(values))
        when(mockAuditService.rtiiAudit(any(), any())(any()))
            .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }
      "the service returns a successful no match with a match pattern of 0" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        val expectedDesResponse = DesSuccessResponse(0, None)
        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }
    }

    "Return 400" when {
      "the request contains an unexpected matching field" in {  //TESTING SCHEMA
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidMatchingFieldDwpRequest))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(Constants.responseInvalidPayload)
      }


      "the request contains an unexpected filter field" in { //TESTING SCHEMA
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidFilterFieldDwpRequest))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(Constants.responseInvalidPayload)
      }

      "the filter fields array is empty" in { //TESTING SCHEMA
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDwpEmptyFieldsRequest))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(Constants.responseInvalidPayload)
      }

      "the filter fields array contains duplicate fields" in { //TESTING SCHEMA
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDwpDuplicateFields))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(Constants.responseInvalidPayload)
      }

      "the filter fields array contains an empty string field" in { //TESTING SCHEMA
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleInvalidDwpEmptyStringField))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(Constants.responseInvalidPayload)
      }

      "the service returns a single error response" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleDwpRequest))

        val expectedDesResponse = Constants.responseInvalidCorrelationId
        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "the service returns multiple error responses" in {

      }

      "the correlationId is invalid" in {

      }

      "the toDate is before fromDate" in {

      }

      "the toDate is equal to fromDate" in {

      }

      "a date is in the wrong format" in {

      }
      "either fromDate or toDate is not defined in the request" in {

      }

      "the nino is invalid" in {

      }
    }

    "Return 403 (FORBIDDEN)" when {
      "A non privileged application attempts to call the endpoint" in {

      }
    }

    "Return 404 (NOT_FOUND)" when {
      "The remote endpoint has indicated that there is no data for the Nino" in {

      }

    }

    "Return 500 (SERVER_ERROR)" when {
      "DES is currently experiencing problems that require live service intervention." in {

      }
    }

    "Return 503 (SERVICE_UNAVAILABLE)" when {
      "Dependent systems are currently not responding" in {

      }

      "DesConnector has thrown an Exception" in {

      }
    }

    "Return INTERNAL_SERVER_ERROR" when {
      "DES has given an unexpected response" in {

      }

      "DES has given a failure code and reason that do not match schema" in {

      }
    }
  }
}
