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

import play.api.Configuration
import uk.gov.hmrc.play.test.UnitSpec
import utils.BaseSpec

class APIAccessConfigSpec extends BaseSpec {

  val emptyAccessConfig: APIAccessConfig = APIAccessConfig(None)
  val accessType: String = "someType"
  val nonPrivateAccessConfig: APIAccessConfig = APIAccessConfig(Some(Configuration("type" -> accessType)))

  "accessType" must {
    "return type from config value" when {
      "config value is defined" in {
        nonPrivateAccessConfig.accessType mustBe accessType
      }
    }

    "return PRIVATE" when {
      "config value is not defined" in {
        val accessConfig = APIAccessConfig(Some(Configuration.from(Map.empty)))
        accessConfig.accessType mustBe "PRIVATE"
      }

      "config is not defined" in {
        emptyAccessConfig.accessType mustBe "PRIVATE"
      }
    }
  }

  "whiteListedApplicationIds" must {
    "return sequence from config" when {
      "config values are defined and accessType is PRIVATE" in {
        val appIds = Seq("abc", "def")
        val accessConfig = APIAccessConfig(Some(Configuration(
          "type" -> "PRIVATE",
          "whitelistedApplicationIds" -> appIds
        )))

        accessConfig.whiteListedApplicationIds mustBe Some(appIds)
      }
    }

    "return empty sequence" when {
      "config value is not defined and accessType is PRIVATE" in {
        val privateAccessConfig: APIAccessConfig = APIAccessConfig(Some(Configuration("type" -> "PRIVATE")))
        privateAccessConfig.whiteListedApplicationIds mustBe Some(Seq())
      }

      "config is not defined and accessType is PRIVATE" in {
        emptyAccessConfig.whiteListedApplicationIds mustBe Some(Seq())
      }
    }

    "return None" when {
      "accessType is not PRIVATE" in {
        nonPrivateAccessConfig.whiteListedApplicationIds mustBe None
      }
    }
  }
}
