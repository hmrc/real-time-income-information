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

package controllers

import akka.stream.Materializer
import config.AppContext
import models.api.APIAccess
import play.api.Configuration
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import views._

class DefinitionControllerSpec extends UnitSpec with WithFakeApplication {

  private implicit val materializer: Materializer = fakeApplication.materializer

  private val apiScope = "scope"
  private val apiContext = "context"
  private val apiWhitelist = "whitelist"
  private val apiAccess = APIAccess("PRIVATE", Some(Seq()))
  private val appContext = new AppContext(Configuration("api.definition.scope" -> apiScope, "api.context" -> apiContext, "api.whitelistedApplicationIds" -> apiWhitelist))
  private val controller = new DefinitionController(appContext)

  "DefinitionController.definition" should {
    lazy val result = getDefinition(controller)

    "return OK status" in {
      status(result) shouldBe OK
    }

    "have a JSON content type" in {
      result.header.headers should contain (CONTENT_TYPE -> "application/json;charset=utf-8")
    }

    "return definition in the body" in {
      jsonBodyOf(result) shouldBe Json.parse(txt.definition(apiAccess, apiContext).toString())
    }
  }

  private def getDefinition(controller: DefinitionController) = {
    await(controller.get().apply(FakeRequest("GET", "/api/definition")))
  }

}