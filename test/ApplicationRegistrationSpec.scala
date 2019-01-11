/*
 * Copyright 2019 HM Revenue & Customs
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

import app.ApplicationRegistration
import config.ApplicationConfig
import connectors.ServiceLocatorConnector
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.test.UnitSpec

class ApplicationRegistrationSpec extends UnitSpec with MockitoSugar {

  case class TestServiceConfiguration(outcome: Boolean) extends ApplicationConfig(mock[Configuration], mock[Environment]) {
    override def getConfBool(confKey: String, defBool: => Boolean): Boolean = outcome
  }

  trait Setup {
    val mockConnector: ServiceLocatorConnector = mock[ServiceLocatorConnector]
  }

  "ApplicationRegistration" should {
    "try to register on service locator if configured" in new Setup {
      val a = new ApplicationRegistration(mockConnector, TestServiceConfiguration(true))
      a.registrationEnabled should be(true)
      verify(mockConnector).register()
    }

    "not try to register on service locator if not configured" in new Setup {
      val a = new ApplicationRegistration(mockConnector, TestServiceConfiguration(false))
      a.registrationEnabled should be(false)
      verify(mockConnector, never()).register()
    }
  }
}

