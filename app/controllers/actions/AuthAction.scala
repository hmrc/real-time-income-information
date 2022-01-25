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

package controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import play.api.Logging
import play.api.libs.json.Json.toJson
import play.api.mvc.Results.{Forbidden, InternalServerError}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Constants._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

final case class AuthDetails(enrolments: Enrolments)
final case class AuthenticatedRequest[A](request: Request[A], authDetails: AuthDetails) extends WrappedRequest[A](request)

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

class AuthActionImpl @Inject()(override val authConnector: AuthConnector, val parser: BodyParsers.Default)
                              (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(AuthProviders(PrivilegedApplication))
      .retrieve(Retrievals.allEnrolments) { enrolments =>
        block(AuthenticatedRequest(request, AuthDetails(enrolments)))
    } recover {
      case _: UnsupportedAuthProvider => Forbidden(toJson(responseNonPrivilegedApplication))
      case e: AuthorisationException  =>
        //$COVERAGE-OFF$
        logger.warn(e.reason)
        //$COVERAGE-ON$
        Forbidden(toJson(forbiddenWithMsg(e.reason)))
      case NonFatal(e) =>
        //$COVERAGE-OFF$
        logger.error("Unexpected exception when authorising", e)
        //$COVERAGE-ON$
        InternalServerError(toJson(responseServiceUnavailable))
    }
  }
}