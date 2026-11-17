/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.actions.AuthAction
import controllers.RealTimeIncomeInformationController
import models.*
import models.ErrorCode.{INVALID_CORRELATION_ID, NOT_FOUND_NINO, SERVER_ERROR}
import models.ErrorResponse.{errorResponseFormat, invalidPayloadWithMsg, serviceUnavailableError}
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.api.test.{FakeHeaders, FakeRequest, Injecting}
import services.RealTimeIncomeInformationService
import testUtils.{BaseSpec, FakeAuthAction}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class RealTimeIncomeInformationControllerSpec
  extends BaseSpec
    with GuiceOneAppPerSuite
    with Injecting
    with BeforeAndAfterEach {

  val correlationId: String = generateUUId
  val nino: String = generateNino

  val mockRtiiService: RealTimeIncomeInformationService = mock[RealTimeIncomeInformationService]
  implicit val mat: Materializer = app.materializer
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[RealTimeIncomeInformationService].toInstance(mockRtiiService),
        bind[AuthAction].to[FakeAuthAction]
      ).configure(
      "api.serviceName" -> Seq("serviceName")
    )
      .build()
  val controller: RealTimeIncomeInformationController = inject[RealTimeIncomeInformationController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRtiiService)
  }

  def fakeRequest(jsonBody: JsValue): FakeRequest[JsValue] =
    FakeRequest(
      method = "POST",
      uri = "",
      headers = FakeHeaders(Seq("Content-type" -> "application/json")),
      body = jsonBody
    )

  val requestDetails: RequestDetails = exampleDwpRequest(nino).as[RequestDetails]

  "retrieveRTII" must {
    "return OK provided a valid request" when {
      "the service returns a successful response" in {
        val expectedDesResponse = RTIISuccessResponse(63, Some(List(Json.obj(
          "surname" -> "Surname",
          "nationalInsuranceNumber" -> nino
        ))))

        when(mockRtiiService.validateRequestDetails(meq(requestDetails))(using any()))
          .thenReturn(List.empty)
        when(mockRtiiService.retrieveCitizenIncome(meq(correlationId),meq(requestDetails))(using any, any))
          .thenReturn(Future.successful(Right(expectedDesResponse)))

        val result: Future[Result] = controller.retrieveRTII(correlationId)(fakeRequest(exampleDwpRequest(nino)))
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }
    }

    "return Bad Request" when {
      "the request has an invalid correlationId" in {
        val result: Future[Result] = controller.retrieveRTII("$$$$$$$$$$$$")(fakeRequest(JsObject.empty))
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(ErrorResponse(INVALID_CORRELATION_ID, "Submission has not passed validation. Invalid header CorrelationId."))
      }

      "the request body cannot be parsed" in {
        val result: Future[Result] = controller.retrieveRTII(correlationId)(fakeRequest(JsObject.empty))
        status(result) mustBe BAD_REQUEST
        status(result) mustBe BAD_REQUEST
        val jsonResult = contentAsJson(result).toString()
        assert(jsonResult.contains("INVALID_PAYLOAD"))
        assert(jsonResult.contains("Invalid Payload."))
        assert(jsonResult.contains("surname"))
        assert(jsonResult.contains("nino"))
        assert(jsonResult.contains("toDate"))
        assert(jsonResult.contains("fromDate"))
        assert(jsonResult.contains("serviceName"))
        assert(jsonResult.contains("filterFields"))
      }

      "the request validation returns a single failure response" in {
        val errors = List(invalidPayloadWithMsg("date not valid"))
        when(mockRtiiService.validateRequestDetails(meq(requestDetails))(using any()))
          .thenReturn(errors)

        val result: Future[Result] = controller.retrieveRTII(correlationId)(fakeRequest(exampleDwpRequest(nino)))
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(errors.head)
      }

      "the request validation returns multiple error responses" in {
        val errors = List(invalidPayloadWithMsg("date not valid"), invalidPayloadWithMsg("name not valid"))
        when(mockRtiiService.validateRequestDetails(meq(requestDetails))(using any()))
          .thenReturn(errors)

        val result: Future[Result] = controller.retrieveRTII(correlationId)(fakeRequest(exampleDwpRequest(nino)))
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(MultipleFailureResponse(errors))
      }
    }

    "Return Not Found" when {
      "nino could not be found downstream" in {
        when(mockRtiiService.validateRequestDetails(meq(requestDetails))(using any()))
          .thenReturn(List.empty)
        when(mockRtiiService.retrieveCitizenIncome(meq(correlationId), meq(requestDetails))(using any, any))
          .thenReturn(Future.successful(Left(UpstreamErrorResponse(statusCode = NOT_FOUND, message = "NOT_FOUND_NINO"))))

        val result: Future[Result] = controller.retrieveRTII(correlationId)(fakeRequest(exampleDwpRequest(nino)))
        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.toJson(ErrorResponse(NOT_FOUND_NINO, "The remote endpoint has indicated that the Nino cannot be found."))
      }

      "data for the nino could not be found downstream" in {
        when(mockRtiiService.validateRequestDetails(meq(requestDetails))(using any()))
          .thenReturn(List.empty)
        when(mockRtiiService.retrieveCitizenIncome(meq(correlationId), meq(requestDetails))(using any, any))
          .thenReturn(Future.successful(Left(UpstreamErrorResponse(statusCode = NOT_FOUND, message = "Data not found"))))

        val result: Future[Result] = controller.retrieveRTII(correlationId)(fakeRequest(exampleDwpRequest(nino)))
        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.toJson(ErrorResponse(ErrorCode.NOT_FOUND, "The remote endpoint has indicated that there is no data for the Nino."))
      }
    }

    "return service unavailable" when {
      "the service returns a service_unavailable upstream error" in {
        when(mockRtiiService.validateRequestDetails(meq(requestDetails))(using any()))
          .thenReturn(List.empty)
        when(mockRtiiService.retrieveCitizenIncome(meq(correlationId), meq(requestDetails))(using any, any))
          .thenReturn(Future.successful(Left(UpstreamErrorResponse(statusCode = SERVICE_UNAVAILABLE, message = ""))))

        val result = controller.retrieveRTII(correlationId)(fakeRequest(exampleDwpRequest(nino)))

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsJson(result) mustBe Json.toJson(serviceUnavailableError)
      }
    }

    "return too many requests" when {
      "the service returns a too many requests upstream error" in {
        when(mockRtiiService.validateRequestDetails(meq(requestDetails))(using any()))
          .thenReturn(List.empty)
        when(mockRtiiService.retrieveCitizenIncome(meq(correlationId), meq(requestDetails))(using any, any))
          .thenReturn(Future.successful(Left(UpstreamErrorResponse(statusCode = TOO_MANY_REQUESTS, message = ""))))

        val result = controller.retrieveRTII(correlationId)(fakeRequest(exampleDwpRequest(nino)))

        status(result) mustBe TOO_MANY_REQUESTS
      }
    }

    "Return Internal Server Error" when {
      "the service returns any other upstream error" in {
        when(mockRtiiService.validateRequestDetails(meq(requestDetails))(using any()))
          .thenReturn(List.empty)
        when(mockRtiiService.retrieveCitizenIncome(meq(correlationId), meq(requestDetails))(using any, any))
          .thenReturn(Future.successful(Left(UpstreamErrorResponse(statusCode = IM_A_TEAPOT, message = ""))))

        val result = controller.retrieveRTII(correlationId)(fakeRequest(exampleDwpRequest(nino)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.toJson(ErrorResponse(SERVER_ERROR, "Internal server error"))
      }
    }
  }
}
