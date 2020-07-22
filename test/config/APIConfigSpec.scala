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

package config

import java.util.UUID

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import utils.BaseSpec

class APIConfigSpec extends BaseSpec with GuiceOneAppPerSuite {

  val realConfig: Configuration = app.configuration

  def SUT(accessType: Option[String] = None, whitelist: List[String] = Nil): APIConfig =
    accessType.fold(new APIConfig(Configuration())){ _type =>
      new APIConfig(
        realConfig ++ Configuration(
          "api.access.type" -> _type,
          "api.access.whitelistedApplicationIds" -> whitelist)
      )
    }

  "apiTypeAccess" must {
    "return PRIVATE" when {
      "there is no configuration" in {
        val config = SUT().apiTypeAccess

        config mustBe "PRIVATE"
      }
      "PRIVATE is configured" in {
        val config = SUT(Some("PRIVATE")).apiTypeAccess

        config mustBe "PRIVATE"
      }
    }
    "return the config" when {
      "something other than PRIVATE is configured" in {
        val randConfigValue = UUID.randomUUID().toString
        val config = SUT(Some(randConfigValue)).apiTypeAccess

        config mustBe randConfigValue
      }
    }
  }

  "apiWhiteList" must {
    "return None" when {
      "there is no configuration" in {
        val config = SUT().apiWhiteList

        config mustBe None
      }
      "PRIVATE is configured" in {
        val config = SUT(Some("PRIVATE"), Nil).apiWhiteList

        config mustBe Some(Nil)
      }

      "PRIVATE is configured and the list has elements" in {
        val config = SUT(Some("PRIVATE"), List("white", "list")).apiWhiteList

        config mustBe Some(Seq("white", "list"))
      }
    }
    "return the config" when {
      "something other than PRIVATE is configured and the list is empty" in {
        val config = SUT(Some("PUBLIC")).apiWhiteList

        config mustBe None
      }
    }
  }
}
