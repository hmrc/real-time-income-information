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

package services

import java.util.UUID

import connectors.DesConnector
import models.RequestDetails
import models.response.{DesFilteredSuccessResponse, DesSingleFailureResponse, DesSuccessResponse}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import scala.concurrent.Future

class RealTimeIncomeInformationServiceSpec extends BaseSpec with ScalaFutures {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val correlationId = UUID.randomUUID().toString

  val mockDesConnector: DesConnector = mock[DesConnector]
  val service: RealTimeIncomeInformationService = new RealTimeIncomeInformationService(mockDesConnector)

  val taxYear: JsValue = Json.parse(
    """
      |    {
      |      "taxYear": "16-17",
      |      "taxYearIndicator": "P",
      |      "hmrcOfficeNumber": "099",
      |      "employerPayeRef": "A1B2c3d4e5",
      |      "employerName1": "Employer",
      |      "nationalInsuranceNumber": "AB123456C",
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

  val filteredTaxYearJson: JsValue = Json.parse(
    """
      |{
      |"surname": "Surname",
      |"nationalInsuranceNumber":"AB123456C"
      |}
    """.stripMargin)

  val filterFields: List[String] = List("surname", "nationalInsuranceNumber")
  val requestDetails = RequestDetails("AB123456C", "serviceName", "2016-12-31", "2017-12-31", "Smith", None, None, None, None, None, filterFields)

  val desResponseWithZeroTaxYears = DesSuccessResponse(63, None)
  val desResponseWithOneTaxYear = DesSuccessResponse(63, Some(List(taxYear)))
  val desResponseWithTwoTaxYears = DesSuccessResponse(63, Some(List(taxYear, taxYear)))

  "pickOneValue is called" must {
    "return the corresponding value if the requested key is present in the given DesSuccessResponse object" in {
      val result = service.pickOneValue("surname", taxYear)
      result shouldBe Some("surname" -> JsString("Surname"))
    }

    "return None if the requested key is not present in the given DesSuccessResponse object" in {
      val result = service.pickOneValue("paymentNoLongerValid", taxYear)
      result shouldBe None
    }
  }

  "pickAll is called" when {
    "when a single tax year is requested" must {
      List(filterFields, "paymentNoLongerValid" :: filterFields).foreach {
        filterFields =>
          s"return available values when requesting filter fields ${filterFields.mkString(", ")}" in {
            val result = service.pickAll(filterFields, desResponseWithOneTaxYear)

            result shouldBe List(filteredTaxYearJson)
          }
      }
    }

    "when multiple tax years are requested" must {
      "return all requested values when all keys are present and the data covers multiple years" in {
        val result = service.pickAll(filterFields, desResponseWithTwoTaxYears)

        result shouldBe List(filteredTaxYearJson, filteredTaxYearJson)
      }
    }

    "when no tax year is requested" must {
      "return an empty list" in {
        val result = service.pickAll(filterFields, desResponseWithZeroTaxYears)

        result shouldBe Nil
      }
    }
  }

  "retrieve citizen income is called" when {
    "given a DES success response with a match" must {
      "retrieve and filter data to return as a DesFilteredSuccessResponse" in {
        val desMatchingRequest = RequestDetails.toMatchingRequest(requestDetails)

        when(mockDesConnector.retrieveCitizenIncome(any(), any(), any())(any())).thenReturn(Future.successful(DesSuccessResponse(63, Some(List(taxYear)))))

        whenReady(service.retrieveCitizenIncome(requestDetails, correlationId)) {
          result => result shouldBe DesFilteredSuccessResponse(63, List(filteredTaxYearJson))
        }
        verify(mockDesConnector, times(1)).retrieveCitizenIncome(any(), ArgumentMatchers.eq(desMatchingRequest), any())(any())
      }
    }
  }

  "given a DES success response with no match" must {
    "return an unfiltered DesSuccessResponse" in {
      when(mockDesConnector.retrieveCitizenIncome(any(), any(), any())(any())).thenReturn(Future.successful(DesSuccessResponse(0, None)))

      whenReady(service.retrieveCitizenIncome(requestDetails, correlationId)) {
        result => result shouldBe DesSuccessResponse(0, None)
      }
    }
  }

  "given a DES failure response return an appropriate error message" in {
    when(mockDesConnector.retrieveCitizenIncome(any(), any(), any())(any())).thenReturn(Future.successful(DesSingleFailureResponse("INVALID_NINO", "Submission has not passed validation. Invalid parameter nino.")))

    whenReady(service.retrieveCitizenIncome(requestDetails, correlationId)) {
      result => result shouldBe DesSingleFailureResponse("INVALID_NINO", "Submission has not passed validation. Invalid parameter nino.")
    }
  }
}
