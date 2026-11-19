/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import config.APIConfig
import connectors.RTIIConnector
import models.*
import models.ErrorCode.*
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.*
import org.scalactic.Prettifier.default
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.*
import play.api.test.FakeRequest
import testUtils.BaseSpec
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RealTimeIncomeInformationServiceSpec extends BaseSpec with BeforeAndAfterEach{

  implicit val hc: HeaderCarrier                = HeaderCarrier()
  val mockRTIIConnector: RTIIConnector            = mock[RTIIConnector]
  val mockAuditService: AuditService  = mock[AuditService]
  val mockAPIConfig: APIConfig = mock[APIConfig]
  val service: RealTimeIncomeInformationService = new RealTimeIncomeInformationService(mockRTIIConnector, mockAuditService, mockAPIConfig)

  val correlationId: String                     = generateUUId
  val nino: String                              = generateNino
  val taxYear: JsValue                          = Json.parse(s"""
                                       |    {
                                       |      "taxYear": "16-17",
                                       |      "taxYearIndicator": "P",
                                       |      "hmrcOfficeNumber": "099",
                                       |      "employerPayeRef": "A1B2c3d4e5",
                                       |      "employerName1": "Employer",
                                       |      "nationalInsuranceNumber": "$nino",
                                       |      "surname": "Surname",
                                       |      "gender": "M",
                                       |      "uniqueEmploymentSequenceNumber": 9999,
                                       |      "taxablePayInPeriod": 999999.99,
                                       |      "taxDeductedOrRefunded": -12345.67,
                                       |      "grossEarningsForNICs": 888888.66,
                                       |      "taxablePayToDate": 999999.99,
                                       |      "totalTaxToDate": 654321.08,
                                       |      "numberOfNormalHoursWorked": "E",
                                       |      "payFrequency": "M1",
                                       |      "paymentDate": "2017-02-03",
                                       |      "earningsPeriodsCovered": 11,
                                       |      "uniquePaymentId": 777777,
                                       |      "paymentConfidenceStatus": "1",
                                       |      "taxCode": "11100L",
                                       |      "hmrcReceiptTimestamp": "2018-04-16T09:23:55Z",
                                       |      "rtiReceivedDate": "2018-04-16",
                                       |      "apiAvailableTimestamp": "2018-04-16T09:23:55Z"
                                       |    }
    """.stripMargin)
  val filteredTaxYearJson: JsValue = Json.parse(s"""
                                                   |{
                                                   |"taxablePayInPeriod": 999999.99,
                                                   |"nationalInsuranceNumber":"$nino"
                                                   |}
    """.stripMargin)
  val filterFields: List[String] = List("taxablePayInPeriod", "nationalInsuranceNumber")
  val requestDetails: RequestDetails =
    RequestDetails(nino, "service", "2016-12-31", "2017-12-31", "Smith", None, None, None, None, None, filterFields)
  val matchPatternInt = 63
  val desMatchingRequest = DownstreamRequest(requestDetails)
  val authenticatedRequest = AuthenticatedRequest(FakeRequest(), Enrolments(Set(Enrolment("filter:real-time-income-information-sg"))))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService)
    reset(mockRTIIConnector)
  }

  "transform" when {
    "a single tax year is given" must {
      List(filterFields, "paymentNoLongerValid" :: filterFields).foreach { filterFields =>
        s"return available values when requesting filter fields ${filterFields.mkString(", ")}" in {
          val result = service.transform(filterFields, RTIISuccessResponse(matchPatternInt, Some(List(taxYear))))
          result.matchPattern mustBe matchPatternInt
          result.taxYears mustBe Some(List(filteredTaxYearJson))
        }
      }
    }
    "multiple tax years are given" must {
      "return all requested values when all keys are present and the data covers multiple years" in {
        val result = service.transform(filterFields, RTIISuccessResponse(matchPatternInt, Some(List(taxYear, taxYear))))
        result.matchPattern mustBe matchPatternInt
        result.taxYears mustBe Some(List(filteredTaxYearJson, filteredTaxYearJson))

      }
    }
    "no tax year is given" must {
      "return an empty list" in {
        val result = service.transform(filterFields, RTIISuccessResponse(matchPatternInt, Some(List.empty)))
        result.matchPattern mustBe matchPatternInt
        result.taxYears mustBe None
      }
    }
  }

  "retrieveCitizenIncome" when {
    "given a success response with a tax year" must {
      "retrieve and filter data to return as a filtered success response" in {
        when(mockRTIIConnector.retrieveCitizenIncome(meq(nino), meq(desMatchingRequest))(using any()))
          .thenReturn(Future.successful(Right(RTIISuccessResponse(63, Some(List(taxYear))))))
        when(mockAuditService.rtiiAudit(any(), any())(using any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockAPIConfig.findScope(meq("filter:real-time-income-information-sg")))
          .thenReturn(Some(ApiScope("filter:real-time-income-information-sg", Set("taxablePayInPeriod"))))

        val result = await(service.retrieveCitizenIncome(correlationId, requestDetails)(using hc, authenticatedRequest))
        result.isRight mustBe true
        result.toOption.get.matchPattern mustBe matchPatternInt
        result.toOption.get.taxYears.get mustBe List(Json.parse(s"""
                                                                   |{
                                                                   |"taxablePayInPeriod": 999999.99
                                                                   |}
    """.stripMargin))
        verify(mockRTIIConnector, times(1)).retrieveCitizenIncome(meq(nino), meq(desMatchingRequest))(using any())
        verify(mockAuditService, times(1)).rtiiAudit(meq(correlationId), meq(requestDetails))(using any())
      }
    }
    "given a DES success response with no tax years" must {
      "return an unfiltered RTIISuccessResponse" in {
        when(mockRTIIConnector.retrieveCitizenIncome(meq(nino), meq(desMatchingRequest))(using any()))
          .thenReturn(Future.successful(Right(RTIISuccessResponse(0, Some(List.empty)))))
        when(mockAuditService.rtiiAudit(any(), any())(using any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockAPIConfig.findScope(meq("filter:real-time-income-information-sg")))
          .thenReturn(Some(ApiScope("filter:real-time-income-information-sg", filterFields.toSet)))
        val result = await(service.retrieveCitizenIncome(correlationId, requestDetails)(using hc, authenticatedRequest))
        result.isRight mustBe true
        result.toOption.get.matchPattern mustBe 0
        result.toOption.get.taxYears mustBe None
        verify(mockRTIIConnector, times(1)).retrieveCitizenIncome(meq(nino), meq(desMatchingRequest))(using any())
        verify(mockAuditService, times(1)).rtiiAudit(meq(correlationId), meq(requestDetails))(using any())
      }
    }
    "given a DES failure response return an appropriate error message" in {
      when(mockRTIIConnector.retrieveCitizenIncome(meq(nino), meq(desMatchingRequest))(using any())).thenReturn(
        Future.successful(Left[UpstreamErrorResponse, RTIISuccessResponse](UpstreamErrorResponse("Not found", NOT_FOUND)))
        )
      when(mockAuditService.rtiiAudit(any(), any())(using any())).thenReturn(Future.successful(AuditResult.Success))

      val result = await(service.retrieveCitizenIncome(correlationId, requestDetails)(using hc, authenticatedRequest))
      verify(mockAuditService, times(1)).rtiiAudit(meq(correlationId), meq(requestDetails))(using any())
      result.isRight mustBe false
      val errorResult = result.swap.toOption.get
      errorResult.statusCode mustBe NOT_FOUND
    }
  }

  "validateRequestDetails" when {
    "given a valid request" must {
      "return an empty list" in {
        when(mockAPIConfig.serviceNames)
          .thenReturn(Seq("service"))
        val result = service.validateRequestDetails(requestDetails)(using authenticatedRequest)
        result mustBe List.empty
      }
    }
    "given a request with invalid and unequal date range" must {
      "return invalid date range error" in {
        when(mockAPIConfig.serviceNames)
          .thenReturn(Seq("service"))
        val requestDetails1 = requestDetails.copy(fromDate = "2018-12-31", toDate = "2017-12-31")
        val result = service.validateRequestDetails(requestDetails1)(using authenticatedRequest)
        result mustBe List(ErrorResponse(INVALID_DATE_RANGE, "The remote endpoint has indicated that the date range is invalid."))
      }
    }
    "given a request with equal date range" must {
      "return invalid date range error" in {
        when(mockAPIConfig.serviceNames)
          .thenReturn(Seq("service"))
        val requestDetails1 = requestDetails.copy(fromDate = "2018-12-31", toDate = "2018-12-31")
        val result = service.validateRequestDetails(requestDetails1)(using authenticatedRequest)
        result mustBe List(ErrorResponse(INVALID_DATES_EQUAL, "The remote endpoint has indicated that the from and to dates are the same."))
      }
    }
    "given a request with invalid field" must {
      "return a list of all the errors" in {
        when(mockAPIConfig.serviceNames)
          .thenReturn(Seq("service"))
        val requestDetails = RequestDetails(
          nino = "test",
          serviceName = "test",
          fromDate = "01-03-2020",
          toDate = "2018/12/31",
          surname = "",
          firstName = Some(""),
          middleName = Some("535676"),
          gender = Some("x"),
          initials = Some("%££$"),
          dateOfBirth = Some("sdf"),
          filterFields = List("surname"))
        val result = service.validateRequestDetails(requestDetails)(using authenticatedRequest)
        val expectedResult = List("nino", "toDate", "fromDate", "surname", "middleName", "firstName", "gender", "initials", "dateOfBirth", "serviceName", "filter-fields").map(x => ErrorResponse(INVALID_PAYLOAD, s"requirement failed: Submission has not passed validation. Invalid $x in payload."))
        result must contain theSameElementsAs expectedResult
      }
    }
  }
}
