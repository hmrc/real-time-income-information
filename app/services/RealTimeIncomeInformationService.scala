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

package services

import com.google.inject.{Inject, Singleton}
import config.APIConfig
import connectors.DesConnector
import models.*
import models.ErrorCode.{INVALID_DATES_EQUAL, INVALID_DATE_RANGE}
import models.ErrorResponse.*
import play.api.libs.json.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.Constants.*

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RealTimeIncomeInformationService @Inject() (desConnector: DesConnector,
                                                  auditService: AuditService)(implicit
    ec: ExecutionContext
) {

  private def usableFields(filterFields: List[String])(implicit ar: AuthenticatedRequest[?]): List[String] = {
    val enrolments = ar.authEnrolments.enrolments.map(_.key)
    val scopes = enrolments.flatMap(APIConfig.findScope) //finds scopes that match enrolment
    val accessibleFields = scopes.flatMap(_.fields) //gets accessible filter fields for valid scopes
    filterFields.filter(accessibleFields.contains) //finds all the fiter fields in the request that are accessible
  }

  def validateRequestDetails(r: RequestDetails)(implicit ar: AuthenticatedRequest[?]): List[ErrorResponse] = {
    val validations: Map[String, Boolean] = Map(
      "nino" -> r.nino.matches(ninoRegex),
      "toDate" -> r.toDate.matches(dateRegex),
      "fromDate" -> r.fromDate.matches(dateRegex),
      "surname" -> r.surname.matches(stringRegex),
      "middleName" -> r.middleName.forall(_.matches(stringRegex)),
      "firstName" -> r.firstName.forall(_.matches(stringRegex)),
      "gender" -> r.gender.forall(_.matches(genderRegex)),
      "initials" -> r.initials.forall(_.matches(stringRegex)),
      "dateOfBirth" -> r.dateOfBirth.forall(_.matches(dateRegex)),
      "serviceName" -> APIConfig.serviceNames.contains(r.serviceName),
      "filter-fields" -> usableFields(r.filterFields).nonEmpty
    )

    val errorFields:List[String] = validations.collect { case (key, false) => key }.toList

    if (errorFields.nonEmpty)
      errorFields.map(field => invalidPayloadWithMsg(s"requirement failed: Submission has not passed validation. Invalid $field in payload."))
    else
      //validates date
      val endDate = LocalDate.parse(r.toDate)
      val startDate = LocalDate.parse(r.fromDate)
      if (endDate.isBefore(startDate))
        List(ErrorResponse(INVALID_DATE_RANGE,"The remote endpoint has indicated that the date range is invalid."))
      else if (endDate.isEqual(startDate))
        List(ErrorResponse(INVALID_DATES_EQUAL, "The remote endpoint has indicated that the from and to dates are the same."))
      else
        List.empty
  }
  
  private[services] def transform(keys: List[String], successResponse: RTIISuccessResponse): RTIISuccessResponse = {
    val transformedTaxYears = successResponse.taxYears.toList.flatten.map { taxYear =>
      Json.toJson(keys.flatMap(key => taxYear.transform((__ \\ key).json.pick[JsValue]).asOpt.map(key -> _)).toMap)
    }
    if(transformedTaxYears.isEmpty)
      RTIISuccessResponse(successResponse.matchPattern, None)
    else
      RTIISuccessResponse(successResponse.matchPattern, Some(transformedTaxYears))
  }

  def retrieveCitizenIncome(correlationId: String, requestDetails: RequestDetails)(implicit
      hc: HeaderCarrier, ar: AuthenticatedRequest[?]
  ): Future[Either[UpstreamErrorResponse, RTIISuccessResponse]] =
    for {
      _ <-auditService.rtiiAudit(correlationId, requestDetails)
      data <-  desConnector.retrieveCitizenIncome(requestDetails.nino, DesMatchingRequest(requestDetails))
      transformedData = data.map(rtii => transform(usableFields(requestDetails.filterFields), rtii))
    } yield transformedData

}
