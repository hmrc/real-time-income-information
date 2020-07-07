package controllers.actions

import com.google.inject.Inject
import models.{DesSingleFailureResponse, RequestDetails, RequestWithDetails}
import org.joda.time.LocalDate
import play.api.libs.json.JsValue
import play.api.mvc.{ActionBuilder, ActionRefiner, AnyContent, BodyParsers, Request, Result}
import utils.Constants

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ValidateDatesActionImpl @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext) extends ValidateDatesAction {
  override protected def refine[A](request: Request[A]): Future[Either[Result, RequestWithDetails[A]]] = {
    request
  }


  def validateDates(requestBody: JsValue): Either[DesSingleFailureResponse, Boolean] = {
    val tryRequestDetails: Try[RequestDetails] = Try(requestBody.as[RequestDetails])
    //TODO refactor this
    if (tryRequestDetails.isFailure)
      Left(Constants.responseInvalidPayload)
    else {
      val requestDetails: RequestDetails = tryRequestDetails.get
      val toDate = parseAsDate(requestDetails.toDate)
      val fromDate = parseAsDate(requestDetails.fromDate)

      (toDate, fromDate) match {
        case (Some(endDate), Some(startDate)) =>
          val dateRangeValid = startDate.isBefore(endDate)
          val datesEqual = endDate.isEqual(startDate)

          (dateRangeValid, datesEqual) match {
            case (true, false) => Right(true)
            case (false, false) => Left(Constants.responseInvalidDateRange)
            case (_, true) => Left(Constants.responseInvalidDatesEqual)
          }
        case _ => Left(Constants.responseInvalidPayload)
      }
    }
  }

  def parseAsDate(string: String): Option[LocalDate] = {

    Try(new LocalDate(string)) match {
      case Success(date) => Some(date)
      case Failure(_) => None
    }
  }
}

trait ValidateDatesAction extends ActionRefiner[Request, RequestWithDetails] with ActionBuilder[Request, AnyContent]


