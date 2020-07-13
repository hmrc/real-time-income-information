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

package controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.{Forbidden, InternalServerError}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import utils.Constants

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AuthActionImpl @Inject()(val parser: BodyParsers.Default,
                           override val authConnector: AuthConnector)
                          (implicit val executionContext: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  private val logger: Logger = Logger(this.getClass)

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(AuthProviders(PrivilegedApplication)) {
      Future.successful(None)
    } recover {
      case _: UnsupportedAuthProvider => Some(Forbidden(Json.toJson(Constants.responseNonPrivilegedApplication)))
      case e: AuthorisationException  =>
        logger.warn(e.reason)
        Some(Forbidden)
      case NonFatal(e)                =>
        logger.error("Unexpected exception when authorising", e)
        Some(InternalServerError)
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionFilter[Request] with ActionBuilder[Request, AnyContent]