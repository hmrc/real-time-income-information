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
