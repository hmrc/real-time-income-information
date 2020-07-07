package services

import models.{DesSingleFailureResponse, RequestDetails}
import org.joda.time.LocalDate
import play.api.libs.json.JsValue
import utils.Constants

import scala.util.{Failure, Success, Try}

class RequestDetailsService {

  def x(requestDetails: RequestDetails): Option[DesSingleFailureResponse] = {
  }

  private def validateDates(requestBody: JsValue): Either[DesSingleFailureResponse, Boolean] = {
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

  private def parseAsDate(string: String): Option[LocalDate] = {

    Try(new LocalDate(string)) match {
      case Success(date) => Some(date)
      case Failure(_) => None
    }
  }

}
