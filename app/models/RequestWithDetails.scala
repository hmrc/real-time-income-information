package models

import play.api.mvc.{Request, WrappedRequest}

//TODO Name this better
final case class RequestWithDetails[A](request: Request[A], requestDetails: RequestDetails) extends WrappedRequest[A](request)
