/*
 * Copyright 2025 HM Revenue & Customs
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

import com.google.inject.{ImplementedBy, Inject}
import models.{AuthenticatedRequest, ErrorResponse}
import models.ErrorCode.NON_PRIVILEGED_APPLICATION
import play.api.Logging
import play.api.libs.json.Json.toJson
import play.api.mvc.Results.{Forbidden, InternalServerError}
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{GatewayTimeoutException, HeaderCarrier}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.ErrorResponse.*

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

class AuthActionImpl @Inject()(override val authConnector: AuthConnector, val parser: BodyParsers.Default)
                              (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(AuthProviders(PrivilegedApplication))
      .retrieve(Retrievals.allEnrolments) { enrolments =>
        block(AuthenticatedRequest(request, enrolments))
    } recover {
      case e: UnsupportedAuthProvider =>
        //$COVERAGE-OFF$
        logger.warn(s"The request has not passed authentication: ${e.reason}")
        //$COVERAGE-ON$
        Forbidden(toJson(ErrorResponse(NON_PRIVILEGED_APPLICATION, "The remote endpoint has indicated the request has not passed authentication.")))
      case e: AuthorisationException  =>
        //$COVERAGE-OFF$
        logger.warn(s"Authorisation failed: ${e.reason}")
        //$COVERAGE-ON$
        Forbidden(toJson(ErrorResponse(NON_PRIVILEGED_APPLICATION, s"Authorisation failed: ${e.reason}")))
      case e: GatewayTimeoutException =>
        //$COVERAGE-OFF$
        logger.error("Unexpected exception when authorising", e)
        //$COVERAGE-ON$
        InternalServerError(toJson(serviceUnavailableError))
    }
  }
}