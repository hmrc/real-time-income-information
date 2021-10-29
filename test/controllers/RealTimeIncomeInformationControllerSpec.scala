/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.stream.Materializer
import controllers.actions.{AuthAction, ValidateCorrelationId}
import models._
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Injecting}
import services.{AuditService, RealTimeIncomeInformationService, RequestDetailsService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.Constants.errorCodeInvalidPayload
import utils._

import scala.concurrent.Future

class RealTimeIncomeInformationControllerSpec
  extends BaseSpec
    with GuiceOneAppPerSuite
    with Injecting
    with BeforeAndAfterEach
    with ResourceProvider {

  val correlationId: String = generateUUId
  val nino: String = generateNino
  val mockRtiiService: RealTimeIncomeInformationService = mock[RealTimeIncomeInformationService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockRequestDetailsService: RequestDetailsService = mock[RequestDetailsService]
  implicit val mat: Materializer = app.materializer

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[RealTimeIncomeInformationService].toInstance(mockRtiiService),
        bind[AuditService].toInstance(mockAuditService),
        bind[AuthAction].to[FakeAuthAction],
        bind[RequestDetailsService].toInstance(mockRequestDetailsService),
        bind[ValidateCorrelationId].to[FakeValidateCorrelationId],
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRtiiService, mockAuditService, mockRequestDetailsService)
  }

  def fakeRequest(jsonBody: JsValue): FakeRequest[JsValue] =
    FakeRequest(
      method = "POST",
      uri = "",
      headers = FakeHeaders(Seq("Content-type" -> "application/json")),
      body = jsonBody
    )

  val controller: RealTimeIncomeInformationController = inject[RealTimeIncomeInformationController]
  val requestDetails: RequestDetails = modifiedExampleDwpRequest(nino).as[RequestDetails]

  "preSchemaValidation" must {
    "Return OK provided a valid request" when {
      "the service returns a successfully filtered response" in {
        val values = Json.toJson(
          Map(
            "surname" -> "Surname",
            "nationalInsuranceNumber" -> nino
          )
        )

        val expectedDesResponse = DesFilteredSuccessResponse(63, List(values))
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))
        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)

        verify(mockAuditService, times(1)).rtiiAudit(meq(correlationId), meq(requestDetails))(any())
      }

      "the service returns a successful when match pattern is 0 and None is returned" in {
        val expectedDesResponse = DesSuccessResponse(0, None)

        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))
        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }
    }

    "Return Bad Request" when {
      List(
        ("a single error response", Constants.responseInvalidCorrelationId),
        (
          "multiple error responses",
          DesMultipleFailureResponse(List(Constants.responseInvalidCorrelationId, Constants.responseInvalidDateRange))
        ),
        (
          "a single failure response with invalid date range code",
          DesSingleFailureResponse(Constants.errorCodeInvalidDateRange, "")
        ),
        (
          "a single failure response with invalid dates equal code",
          DesSingleFailureResponse(Constants.errorCodeInvalidDatesEqual, "")
        ),
        (
          "a single failure response with invalid payload code",
          DesSingleFailureResponse(Constants.errorCodeInvalidPayload, "")
        )
      ).foreach {
        case (testDescription, expectedDesResponse) =>
          s"the service returns $testDescription" in {
            when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
              .thenReturn(Future.successful(AuditResult.Success))
            when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
              .thenReturn(Future.successful(expectedDesResponse))
            when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
            when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))

            val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))
            status(result) mustBe BAD_REQUEST

            (expectedDesResponse: @unchecked) match {
              case failureResponse: DesSingleFailureResponse =>
                contentAsJson(result) mustBe Json.toJson(failureResponse)
              case failureResponse: DesMultipleFailureResponse =>
                contentAsJson(result) mustBe Json.toJson(failureResponse)
            }
          }
      }

      "Unable to parse json" in {
        val jsonInput: JsValue = JsObject.empty
        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(jsonInput))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(Constants.invalidPayloadWithMsg(
          "Invalid Payload. Invalid fromDate, surname, toDate, filterFields, serviceName, nino"))
      }

      "missing nino in Json, return desciptive error message" in {
        val jsonInput: JsValue = Json.parse(
          """
          {
            "serviceName": "some value",
            "fromDate": "some value",
            "toDate": "some value",
            "surname": "some value"
          }
         """)

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(jsonInput))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(Constants.invalidPayloadWithMsg(
          "Invalid Payload. Invalid nino, filterFields"))
      }

      "missing surname in Json, return desciptive error message" in {
        val jsonInput: JsValue = Json.parse(
          """
          {
            "nino": "some value",
            "serviceName": "some value",
            "fromDate": "some value",
            "toDate": "some value"
          }
        """)
        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(jsonInput))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(Constants.invalidPayloadWithMsg(
          "Invalid Payload. Invalid filterFields, surname"))
      }

      "missing servicename in Json, return desciptive error message" in {
        val jsonInput: JsValue = Json.parse(
          """
          {
            "nino": "some value",
            "fromDate": "some value",
            "toDate": "some value",
            "surname": "some value"
          }
        """)
        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(jsonInput))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(Constants.invalidPayloadWithMsg(
          "Invalid Payload. Invalid filterFields, serviceName"))
      }

      "missing fromDate in Json, return desciptive error message" in {
        val jsonInput: JsValue = Json.parse(
          """
          {
            "nino": "some value",
            "serviceName": "some value",
            "toDate": "some value",
            "surname": "some value"
          }
        """)
        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(jsonInput))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(Constants.invalidPayloadWithMsg(
          "Invalid Payload. Invalid fromDate, filterFields"))
      }

      "missing toDate in Json, return desciptive error message" in {
        val jsonInput: JsValue = Json.parse(
          """
          {
            "nino": "some value",
            "serviceName": "some value",
            "fromDate": "some value",
            "surname": "some value"
          }
        """)
        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(jsonInput))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(Constants.invalidPayloadWithMsg(
          "Invalid Payload. Invalid toDate, filterFields"))
      }
    }

    "Return 404 (NOT_FOUND)" when {
      "The remote endpoint has indicated that there is no data for the Nino" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeNotFoundNino, "")

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))
        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "The controller receives an Error Code Not Found Error from the service layer" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeNotFound, "")

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "json doesn't pass RequestDetails schema validation" in {
        val expectedDesResponse =
          DesSingleFailureResponse(errorCodeInvalidPayload, "requirement failed: Submission has not passed validation. Invalid nino in payload.")

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }
    }

    "Service unavailable" when {
      "The controller receives a failure response from DES in the service layer" in {

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
          .thenReturn(Future.failed(new Exception))
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsJson(result) mustBe Json.toJson(
          DesSingleFailureResponse(
            Constants.errorCodeServiceUnavailable,
            "Dependent systems are currently not responding."
          )
        )
      }

      "The controller receives Des single failure response service unavailable" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeServiceUnavailable, "")

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "The controller receives a DesNoResponse from the service layer" in {
        val expectedDesResponse = DesNoResponse()
        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))

        status(result) mustBe SERVICE_UNAVAILABLE

        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)

      }
    }

    "Return 500 Internal Server Error" when {
      List(
        ("The controller receives a DesUnexpectedResponse from the service layer", DesUnexpectedResponse()),
        (
          "The controller receives an Error Code Server Error from the service layer",
          DesSingleFailureResponse(Constants.errorCodeServerError, "")
        ),
        ("The controller receives an unmatched DES error", DesSingleFailureResponse("", ""))
      ).foreach {
        case (testName, expectedDesResponse) =>
          testName in {

            when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
              .thenReturn(Future.successful(AuditResult.Success))
            when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
              .thenReturn(Future.successful(expectedDesResponse))
            when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
            when(mockRequestDetailsService.processFilterFields(any())(any(), any())).thenReturn(Right(requestDetails))

            val result = controller.preSchemaValidation(correlationId)(fakeRequest(modifiedExampleDwpRequest(nino)))

            status(result) mustBe INTERNAL_SERVER_ERROR

            val expectedJSON: JsValue = (expectedDesResponse: @unchecked) match {
              case expectedResponse: DesSingleFailureResponse => Json.toJson(expectedResponse)
              case expectedResponse: DesUnexpectedResponse => Json.toJson(expectedResponse)
            }
            contentAsJson(result) mustBe expectedJSON
          }
      }
    }
  }
}
