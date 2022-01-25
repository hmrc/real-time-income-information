/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers._

import java.util.UUID
import play.api.Configuration
import utils.BaseSpec

class APIConfigSpec extends BaseSpec {

  val filterFullAccessScope = "filter:real-time-income-information-full"

  "apiScope" must {
    def SUT = new APIConfig(Configuration(
      s"""api.scopes."$filterFullAccessScope".fields""" -> Seq(1,2,3),
      "api.fields.1" -> "A",
      "api.fields.2" -> "B",
      "api.fields.3" -> "C"
    ))

    "return scope" when {
      "findScope with valid configuration" in {

        val scope = SUT.findScope(filterFullAccessScope)

        scope.isDefined mustBe true
        scope.get.name mustBe filterFullAccessScope
        scope.get.fields.map(f => f.id) mustBe Seq(1,2,3)
      }
    }

    "return none" when {
      "scopeName does not exist" in {
        val scopeName = "Test"

        val scope = SUT.findScope(scopeName)

        scope.isDefined mustBe false
      }
    }
  }

  "apiTypeAccess" must {

    def SUT(accessType: Option[String] = None): APIConfig =
      accessType.fold(new APIConfig(Configuration())) { _type =>
        new APIConfig(
          Configuration("api.access.type" -> _type)
        )
      }

    "return PRIVATE" when {
      "there is no configuration" in {
        val config = SUT().apiAccessType

        config mustBe "PRIVATE"
      }

      "PRIVATE is configured" in {
        val config = SUT(Some("PRIVATE")).apiAccessType

        config mustBe "PRIVATE"
      }
    }

    "return the config" when {
      "something other than PRIVATE is configured" in {
        val randConfigValue = UUID.randomUUID().toString
        val config          = SUT(Some(randConfigValue)).apiAccessType

        config mustBe randConfigValue
      }
    }
  }
}
