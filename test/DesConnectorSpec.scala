/*
 * Copyright 2018 HM Revenue & Customs
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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import config.ApplicationConfig
import connectors.DesConnector
import models.{DesMatchingRequest, RequestDetails}
import models.response.{DesMultipleFailureResponse, DesSingleFailureResponse, DesSuccessResponse, DesUnexpectedResponse}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper
import play.api.libs.json._

import scala.util.Random


class DesConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with WireMockHelper with BaseSpec {

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  protected lazy val connector: DesConnector = injector.instanceOf[DesConnector]
  protected lazy val desConfig: ApplicationConfig = injector.instanceOf[ApplicationConfig]

  "DesConnector" must {

    "return a DesSuccessResponse" when {

      "successfully retrieved citizen income data" in {

        val taxYear = Json.parse("""{
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
                         |}""".stripMargin)

        val expectedResponse = DesSuccessResponse(63, Some(List(taxYear)))

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], correlationId)) {
          result => result mustBe expectedResponse
        }

      }

      "received a 200 No match response from DES" in {

        val expectedResponse = DesSuccessResponse(0, None)

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successsNoMatch.toString())
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], correlationId)) {
          result => result mustBe expectedResponse
        }

      }
    }

    "return a single DesFailureResponse with the appropriate code and reason" when {

      "the remote endpoint has indicated that there is no data for the Nino" in {

        val expectedResponse = DesSingleFailureResponse("NOT_FOUND",
          "The remote endpoint has indicated that there is no data for the Nino.")

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              notFound().withBody(noDataFoundNinoJson.toString)
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], correlationId)) {
          result => result mustBe expectedResponse
        }
      }

      "the remote endpoint has indicated that the Nino cannot be found" in {

        val expectedResponse = DesSingleFailureResponse("NOT_FOUND_NINO",
          "The remote endpoint has indicated that the Nino cannot be found.")

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              notFound().withBody(notFoundNinoJson.toString)
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], correlationId)) {
          result => result mustBe expectedResponse
        }
      }

      "the remote endpoint has indicated that the correlation Id is invalid" in {

        val expectedResponse = DesSingleFailureResponse("INVALID_CORRELATION_ID",
          "Submission has not passed validation. Invalid header CorrelationId.")

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              badRequest().withBody(invalidCorrelationIdJson.toString)
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], "invalidcorrelationid")) {
          result => result mustBe expectedResponse
        }
      }

      "DES is currently experiencing problems that require live service intervention" in {

        val expectedResponse = DesSingleFailureResponse("SERVER_ERROR",
          "DES is currently experiencing problems that require live service intervention.")

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serverError().withBody(serverErrorJson.toString)
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], correlationId)) {
          result => result mustBe expectedResponse
        }

      }

      "Dependent systems are currently not responding" in {

        val expectedResponse = DesSingleFailureResponse("SERVICE_UNAVAILABLE",
          "Dependent systems are currently not responding.")

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serviceUnavailable().withBody(serviceUnavailableJson.toString)
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], correlationId)) {
          result => result mustBe expectedResponse
        }

      }
    }

    "Return multiple DESFailureResponse" when {
      "the DES response contains a list of failures" in {
        val responses = DesMultipleFailureResponse(List(
          DesSingleFailureResponse("INVALID_NINO", "Submission has not passed validation. Invalid parameter nino."),
          DesSingleFailureResponse("INVALID_PAYLOAD", "Submission has not passed validation. Invalid Payload.")))

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              badRequest().withBody(multipleErrors.toString)
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], correlationId)) {
          result => result mustBe responses
        }

      }
    }

    "Return a DES unexpected response" when {
      "the DES response doesn't match the schema" in {

        val response = DesUnexpectedResponse()

        val nino = randomNino
        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              serviceUnavailable().withBody("{}")
            )
        )

        whenReady(connector.retrieveCitizenIncome(nino, exampleDesRequest.as[DesMatchingRequest], correlationId)) {
          result => result mustBe response
        }
      }
    }
  }

  "Request Details" must {
    "Form a Des Matching request" when {
      "Given a valid dwp request" in {
        val requestDetails = RequestDetails("AB123456C", "serviceName", "2016-12-31", "2017-12-31", "Smith", None, None, None, None, None, List("surname", "nationalInsuranceNumber"))
        val transformedRequest = RequestDetails.toMatchingRequest(requestDetails)
        transformedRequest mustBe DesMatchingRequest("2016-12-31", "2017-12-31", "Smith", None, None, None, None, None)
      }
    }
  }

  private implicit val hc = HeaderCarrier()

  private def randomNino: String = new Generator(new Random).nextNino.toString()

  private val correlationId = UUID.randomUUID().toString
}
