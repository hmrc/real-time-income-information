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

package connectors

import models.*
import org.apache.pekko.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import repositories.RTIICacheRepository
import testUtils.BaseSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DesConnectorSpec extends BaseSpec {

  val testAuthToken              = "TestAuthToken"
  val testEnv                    = "TestEnv"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino: String               = generateNino
  val correlationId: String      = generateUUId
  val mockDefaultDesConnector: DefaultDesConnector = mock[DefaultDesConnector]
  val mockRepo: RTIICacheRepository = mock[RTIICacheRepository]
  def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[RTIICacheRepository].toInstance(mockRepo),
        bind[DefaultDesConnector].toInstance(mockDefaultDesConnector)
      )
      .build()
  
  def connector: DesConnector = new CachingDesConnector(mockDefaultDesConnector, mockRepo)

  val taxYear: JsValue = Json.parse("""{
                             |      "taxYear": "16-17",
                             |      "taxYearIndicator": "P",
                             |      "hmrcOfficeNumber": "099",
                             |      "employerPayeRef": "A1B2c3d4e5",
                             |      "employerName1": "Employer",
                             |      "nationalInsuranceNumber": "QQ123456C",
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
                             |}""".stripMargin)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockDefaultDesConnector)
    reset(mockRepo)
  }

  "retrieveCitizenIncome" must {
    "return a RTIISuccessResponse" when {
      "data is available in the cache repository" in {
        val expectedResponse = RTIISuccessResponse(63, Some(List(taxYear)))
        when(mockRepo.get[RTIISuccessResponse](any())(any())(using any))
          .thenReturn(Future(Some(expectedResponse)))
        val result = await(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest]))
        result.isRight mustBe true
        result mustBe Right(expectedResponse)
      }

      "data is not available in cache but downstream connector sends a successful response" in {
        val expectedResponse = RTIISuccessResponse(63, Some(List(taxYear)))
        when(mockRepo.get[RTIISuccessResponse](any())(any())(using any))
          .thenReturn(Future(None))
        when(mockDefaultDesConnector.retrieveCitizenIncome(any(), any())(using any))
          .thenReturn(Future(Right(expectedResponse)))
        when(mockRepo.put[RTIISuccessResponse](any())(any(), any())(using any))
          .thenReturn(Future.successful(Ok))
        val result =await(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest]))
        result.isRight mustBe true
        result mustBe Right(expectedResponse)
        verify(mockRepo, times(1)).put[RTIISuccessResponse](any())(any(), any())(using any)
      }
    }
    "return an upstream error response" when {
      "an error response is returned from downstream connector" in {
        val expectedResponse = UpstreamErrorResponse("SampleErrorResponse", 500)
        when(mockRepo.get[RTIISuccessResponse](any())(any())(using any))
          .thenReturn(Future(None))
        when(mockDefaultDesConnector.retrieveCitizenIncome(any(), any())(using any))
          .thenReturn(Future(Left(expectedResponse)))
        val result = await(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest]))
        result.isRight mustBe false
        result mustBe Left(expectedResponse)
        verify(mockRepo, times(0)).put[RTIISuccessResponse](any())(any(), any())(using any)
      }
    }
  }
}
