/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Results}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments, InternalError, UnsupportedAuthProvider}
import utils.{BaseSpec, Constants}

import scala.concurrent.Future

class AuthActionSpec extends BaseSpec with Injecting with GuiceOneAppPerSuite {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()

  object Harness extends Results {
    val authAction: AuthAction = inject[AuthAction]

    def test(): Action[AnyContent] = authAction(_ => Ok)
  }

  "AuthAction" must {
    "return None" when {
      "authenticated" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(Future.successful(Enrolments(Set.empty)))
        val result = Harness.test()(FakeRequest())
        status(result) mustBe OK
      }
    }

    "return forbidden" when {
      "Non Privileged Application" in {
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any()))
          .thenReturn(Future.failed(UnsupportedAuthProvider()))
        val result = Harness.test()(FakeRequest())

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.toJson(Constants.responseNonPrivilegedApplication)
      }

      "unexpected unauthenticated" in {
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any()))
          .thenReturn(Future.failed(InternalError("some error message")))
        val result = Harness.test()(FakeRequest())

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.toJson(Constants.forbiddenWithMsg("some error message"))
      }
    }

    "return internal server error" when {
      "Unexpected exception when authorising" in {
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any()))
          .thenReturn(Future.failed(new Exception()))
        val result = Harness.test()(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
