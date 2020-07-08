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

package utils

import com.google.inject.Inject
import controllers.actions.{ValidateCorrelationId, ValidateCorrelationIdAction}
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

class FakeValidateCorrelationId @Inject()(bodyParser: BodyParsers.Default,
                                          ec: ExecutionContext) extends ValidateCorrelationId {
  override def apply(correlationId: String): ValidateCorrelationIdAction =
    new ValidateCorrelationIdAction {
      override def parser: BodyParser[AnyContent] = bodyParser

      override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful(None)

      override protected def executionContext: ExecutionContext = ec
    }
}
