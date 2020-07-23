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

import akka.stream.Materializer
import controllers.actions.{AuthAction, ValidateCorrelationId}
import models._
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Injecting}
import services.{AuditService, RealTimeIncomeInformationService, RequestDetailsService, SchemaValidator}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{BaseSpec, Constants, FakeAuthAction, FakeValidateCorrelationId}

import scala.concurrent.Future

class RealTimeIncomeInformationControllerSpec
    extends BaseSpec
    with GuiceOneAppPerSuite
    with Injecting
    with BeforeAndAfterEach {

  val correlationId: String                             = generateUUId
  val nino: String                                      = generateNino
  val mockRtiiService: RealTimeIncomeInformationService = mock[RealTimeIncomeInformationService]
  val mockAuditService: AuditService                    = mock[AuditService]
  val mockRequestDetailsService: RequestDetailsService  = mock[RequestDetailsService]
  val mockSchemaValidator: SchemaValidator              = mock[SchemaValidator]
  implicit val mat: Materializer                        = app.materializer

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[RealTimeIncomeInformationService].toInstance(mockRtiiService),
        bind[AuditService].toInstance(mockAuditService),
        bind[AuthAction].to[FakeAuthAction],
        bind[RequestDetailsService].toInstance(mockRequestDetailsService),
        bind[ValidateCorrelationId].to[FakeValidateCorrelationId],
        bind[SchemaValidator].toInstance(mockSchemaValidator)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRtiiService, mockAuditService, mockSchemaValidator, mockRequestDetailsService)
  }

  def fakeRequest(jsonBody: JsValue): FakeRequest[JsValue] =
    FakeRequest(
      method = "POST",
      uri = "",
      headers = FakeHeaders(Seq("Content-type" -> "application/json")),
      body = jsonBody
    )

  val controller: RealTimeIncomeInformationController = inject[RealTimeIncomeInformationController]

  "preSchemaValidation" must {
    "Return OK provided a valid request" when {
      val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]
      "the service returns a successfully filtered response" in {

        val values = Json.toJson(
          Map(
            "surname"                 -> "Surname",
            "nationalInsuranceNumber" -> nino
          )
        )

        val expectedDesResponse = DesFilteredSuccessResponse(63, List(values))
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)

        verify(mockSchemaValidator, times(1)).validate(any())
        verify(mockAuditService, times(1)).rtiiAudit(meq(correlationId), meq(requestDetails))(any())
      }

      "the service returns a successful when match pattern is 0 and None is returned" in {
        val expectedDesResponse = DesSuccessResponse(0, None)

        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))
        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }
    }

    "Return Bad Request" when {

      "the service returns a single error response" in {
        val expectedDesResponse            = Constants.responseInvalidCorrelationId
        val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "the service returns multiple error responses" in {
        val expectedDesResponse =
          DesMultipleFailureResponse(List(Constants.responseInvalidCorrelationId, Constants.responseInvalidDateRange))
        val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(any(), any()))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "the service layer returns a single failure response with invalid date range code" in {
        val expectedDesResponse            = DesSingleFailureResponse(Constants.errorCodeInvalidDateRange, "")
        val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "the service layer returns a single failure response with invalid dates equal code" in {
        val expectedDesResponse            = DesSingleFailureResponse(Constants.errorCodeInvalidDatesEqual, "")
        val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "the service layer returns a single failure response with invalid payload code" in {
        val expectedDesResponse            = DesSingleFailureResponse(Constants.errorCodeInvalidPayload, "")
        val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "Unable to parse json" in {
        val jsonInput: JsValue     = JsObject.empty
        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(jsonInput))

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(Constants.responseInvalidPayload)
      }

      "schemaValidator returns false" in {
        val expectedResponse               = Constants.responseInvalidPayload
        val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(false)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(expectedResponse)
      }
    }

    "Return 404 (NOT_FOUND)" when {
      val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]
      "The remote endpoint has indicated that there is no data for the Nino" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeNotFoundNino, "")

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }

      "The controller receives an Error Code Not Found Error from the service layer" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeNotFound, "")

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }
    }

    "Service unavailable" when {
      val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]
      "The controller receives a failure response from DES in the service layer" in {

        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.failed(new Exception))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

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
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)
      }
    }

    "Return 502 BAD_GATEWAY " when {
      "The controller receives a DesNoResponse from the service layer" in {
        val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]
        val expectedDesResponse            = DesNoResponse()
        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
          .thenReturn(Future.successful(expectedDesResponse))
        when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
        when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) mustBe BAD_GATEWAY

        contentAsJson(result) mustBe Json.toJson(expectedDesResponse)

      }
    }

    "Return 500 Internal Server Error" when {
      val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]
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
            when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId)))
              .thenReturn(Future.successful(expectedDesResponse))
            when(mockSchemaValidator.validate(exampleDwpRequest)).thenReturn(true)
            when(mockRequestDetailsService.validateDates(requestDetails)).thenReturn(Right(requestDetails))

            val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

            status(result) mustBe INTERNAL_SERVER_ERROR

            val expectedJSON: JsValue = (expectedDesResponse: @unchecked) match {
              case expectedResponse: DesSingleFailureResponse => Json.toJson(expectedResponse)
              case expectedResponse: DesUnexpectedResponse    => Json.toJson(expectedResponse)
            }
            contentAsJson(result) mustBe expectedJSON
          }
      }
    }

  }
}
