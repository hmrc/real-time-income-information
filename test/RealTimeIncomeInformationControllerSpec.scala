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

import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import connectors.DesConnector
import controllers.RealTimeIncomeInformationController
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

import scala.util.Random

class RealTimeIncomeInformationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures with WireMockHelper with BaseSpec with IntegrationPatience {

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  protected lazy val connector: DesConnector = injector.instanceOf[DesConnector]
  protected lazy val controller: RealTimeIncomeInformationController = injector.instanceOf[RealTimeIncomeInformationController]

  "RealTimeIncomeInformationController" should {
    "Return 200" when {
      "real time income information is available" in  {

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = Json.toJson(exampleRequest))

        server.stubFor(
          post(urlEqualTo(s"/individuals/$nino/income"))
            .willReturn(
              ok(successMatchOneYear.toString())
            )
        )

        val sut = createSUT(connector)
        val result = sut.retrieveCitizenIncome(nino)(fakeRequest)
        status(result) mustBe 200
      }
    }

  }

  def createSUT(desConnector: DesConnector) =
    new RealTimeIncomeInformationController(desConnector)

  private implicit val hc = HeaderCarrier()

  private def randomNino: Nino = new Generator(new Random).nextNino
  private val nino: String = randomNino.nino

  private val correlationId = UUID.randomUUID()
}
