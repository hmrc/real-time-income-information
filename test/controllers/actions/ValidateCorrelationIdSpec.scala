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

package controllers.actions

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Results}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{BaseSpec, Constants}

class ValidateCorrelationIdSpec extends BaseSpec with Injecting with GuiceOneAppPerSuite {

  val correlationId: String = generateUUId

  object Harness extends Results {
    val validateId: ValidateCorrelationId = inject[ValidateCorrelationId]

    def test(id: String): Action[AnyContent] = validateId(id)(_ => Ok)
  }

  "ValidateCorrelationId" must {
    "validate the id" in {
      val result = Harness.test(correlationId)(FakeRequest())

      status(result) mustBe OK
    }

    "return bad request" when {
      "the correlationId is invalid" in {
        val result = Harness.test("invalidCorrelationId")(FakeRequest())

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(Constants.responseInvalidCorrelationId)
      }
    }
  }
}
