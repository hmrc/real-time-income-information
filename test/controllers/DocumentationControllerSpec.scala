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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Injecting}
import utils.BaseSpec

import scala.concurrent.Future

class DocumentationControllerSpec extends BaseSpec with GuiceOneAppPerSuite with Injecting {

  implicit val materializer: Materializer = app.materializer

  lazy val controller: DocumentationController = inject[DocumentationController]
  lazy val applicationRamlContent: String      = getResourceFileContent("/public/api/conf/1.0/application.raml")

  "DocumentationController" must {
    "return OK status with application.raml in the body" in {
      val result: Future[Result] =
        controller.conf("1.0", "application.raml")(FakeRequest("GET", "/api/conf/1.0/application.raml"))
      status(result) mustBe OK
      contentAsString(result) mustBe applicationRamlContent
    }
  }
}
