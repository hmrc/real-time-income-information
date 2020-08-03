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
import play.api.libs.json.Json.toJson
import play.api.mvc.Results.BadRequest
import play.api.mvc._
import utils.Constants

import scala.concurrent.{ExecutionContext, Future}

class ValidateCorrelationIdImpl @Inject()(_parser: BodyParsers.Default)
                                         (implicit _executionContext: ExecutionContext) extends ValidateCorrelationId {

  override def apply(correlationId: String): ValidateCorrelationIdAction = new ValidateCorrelationIdAction {
    override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
      val correlationIdRegex = """^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$""".r

      val optResult: Option[Result] = correlationId match {
        case correlationIdRegex(_*) => None
        case _ => Some(BadRequest(toJson(Constants.responseInvalidCorrelationId)))
      }

      Future.successful(optResult)
    }

    override def parser: BodyParser[AnyContent] = _parser

    override protected def executionContext: ExecutionContext = _executionContext
  }
}

trait ValidateCorrelationIdAction extends ActionFilter[Request] with ActionBuilder[Request, AnyContent]

@ImplementedBy(classOf[ValidateCorrelationIdImpl])
trait ValidateCorrelationId {
  def apply(correlationId: String): ValidateCorrelationIdAction
}
