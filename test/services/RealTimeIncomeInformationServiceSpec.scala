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

import connectors.DesConnector
import models._
import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito._
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RealTimeIncomeInformationServiceSpec extends BaseSpec {

  implicit val hc: HeaderCarrier                = HeaderCarrier()
  val correlationId: String                     = generateUUId
  val mockDesConnector: DesConnector            = mock[DesConnector]
  val service: RealTimeIncomeInformationService = new RealTimeIncomeInformationService(mockDesConnector)
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
                                                   |"surname": "Surname",
                                                   |"nationalInsuranceNumber":"$nino"
                                                   |}
    """.stripMargin)

  val filterFields: List[String] = List("surname", "nationalInsuranceNumber")

  val requestDetails: RequestDetails =
    RequestDetails(nino, "serviceName", "2016-12-31", "2017-12-31", "Smith", None, None, None, None, None, filterFields)

  val desResponseWithZeroTaxYears: DesSuccessResponse = DesSuccessResponse(63, None)
  val desResponseWithOneTaxYear: DesSuccessResponse   = DesSuccessResponse(63, Some(List(taxYear)))
  val desResponseWithTwoTaxYears: DesSuccessResponse  = DesSuccessResponse(63, Some(List(taxYear, taxYear)))

  "pickOneValue is called" must {
    "return the corresponding value if the requested key is present in the given DesSuccessResponse object" in {
      val result = service.pickOneValue("surname", taxYear)
      result mustBe Some("surname" -> JsString("Surname"))
    }

    "return None if the requested key is not present in the given DesSuccessResponse object" in {
      val result = service.pickOneValue("paymentNoLongerValid", taxYear)
      result mustBe None
    }
  }

  "pickAll is called" when {
    "when a single tax year is requested" must {
      List(filterFields, "paymentNoLongerValid" :: filterFields).foreach { filterFields =>
        s"return available values when requesting filter fields ${filterFields.mkString(", ")}" in {
          val result = service.pickAll(filterFields, desResponseWithOneTaxYear)

          result mustBe List(filteredTaxYearJson)
        }
      }
    }

    "when multiple tax years are requested" must {
      "return all requested values when all keys are present and the data covers multiple years" in {
        val result = service.pickAll(filterFields, desResponseWithTwoTaxYears)

        result mustBe List(filteredTaxYearJson, filteredTaxYearJson)
      }
    }

    "when no tax year is requested" must {
      "return an empty list" in {
        val result = service.pickAll(filterFields, desResponseWithZeroTaxYears)

        result mustBe Nil
      }
    }
  }

  "retrieve citizen income is called" when {
    "given a DES success response with a match" must {
      "retrieve and filter data to return as a DesFilteredSuccessResponse" in {
        val desMatchingRequest = RequestDetails.toMatchingRequest(requestDetails)

        when(mockDesConnector.retrieveCitizenIncome(meq(nino), meq(desMatchingRequest), meq(correlationId)))
          .thenReturn(Future.successful(DesSuccessResponse(63, Some(List(taxYear)))))

        val result: DesResponse = await(service.retrieveCitizenIncome(requestDetails, correlationId))
        result mustBe DesFilteredSuccessResponse(63, List(filteredTaxYearJson))
        verify(mockDesConnector, times(1)).retrieveCitizenIncome(meq(nino), meq(desMatchingRequest), meq(correlationId))
      }
    }

    "given a DES success response with no match" must {
      "return an unfiltered DesSuccessResponse" in {
        val desMatchingRequest = RequestDetails.toMatchingRequest(requestDetails)

        when(mockDesConnector.retrieveCitizenIncome(meq(nino), meq(desMatchingRequest), meq(correlationId)))
          .thenReturn(Future.successful(DesSuccessResponse(0, None)))

        val result: DesResponse = await(service.retrieveCitizenIncome(requestDetails, correlationId))
        result mustBe DesSuccessResponse(0, None)
      }
    }

    "given a DES failure response return an appropriate error message" in {
      val desMatchingRequest = RequestDetails.toMatchingRequest(requestDetails)

      when(mockDesConnector.retrieveCitizenIncome(meq(nino), meq(desMatchingRequest), meq(correlationId))).thenReturn(
        Future.successful(
          DesSingleFailureResponse("INVALID_NINO", "Submission has not passed validation. Invalid parameter nino.")
        )
      )

      val result: DesResponse = await(service.retrieveCitizenIncome(requestDetails, correlationId))
      result mustBe DesSingleFailureResponse(
        "INVALID_NINO",
        "Submission has not passed validation. Invalid parameter nino."
      )
    }
  }
}
