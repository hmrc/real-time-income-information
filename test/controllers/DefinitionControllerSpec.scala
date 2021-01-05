/*
 * Copyright 2021 HM Revenue & Customs
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
import models.api.APIAccess
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers, Injecting}
import Helpers.{contentAsJson, defaultAwaitTimeout, headers, status}
import utils.BaseSpec
import views._

class DefinitionControllerSpec extends BaseSpec with GuiceOneAppPerSuite with Injecting {

  private val apiScope                            = "scope"
  private val apiContext                          = "context"
  private val apiWhitelist                        = "whitelist"
  private val apiAccess                           = APIAccess("PRIVATE", Some(Seq.empty))
  private lazy val controller                     = inject[DefinitionController]
  implicit private val materializer: Materializer = app.materializer

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "api.definition.scope"                 -> apiScope,
        "api.context"                          -> apiContext,
        "api.whitelistedApplicationIds"        -> apiWhitelist,
        "api.access.type"                      -> "PRIVATE",
        "api.access.whitelistedApplicationIds" -> Seq.empty
      )
      .build()

  "get" must {
    "return a Json definition" in {
      val result = controller.get()(FakeRequest("GET", "/api/definition"))

      status(result) mustBe OK
      headers(result) must contain(CONTENT_TYPE -> "application/json;charset=utf-8")
      contentAsJson(result) mustBe Json.parse(txt.definition(apiAccess, apiContext).toString())
    }
  }
}
