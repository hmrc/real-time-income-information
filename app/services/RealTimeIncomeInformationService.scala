/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.DesConnector
import models.{DesFilteredSuccessResponse, DesResponse, DesSuccessResponse, RequestDetails}
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RealTimeIncomeInformationService @Inject() (desConnector: DesConnector)(implicit
    ec: ExecutionContext
) {

  private[services] def pickOneValue(key: String, taxYear: JsValue): Option[(String, JsValue)] =
    taxYear.transform((__ \\ key).json.pick[JsValue]).asOpt.map(key -> _)

  private[services] def pickAll(keys: List[String], desSuccessResponse: DesSuccessResponse): List[JsValue] =
    desSuccessResponse.taxYears.toList.flatten.map { taxYear =>
      Json.toJson(keys.flatMap(key => pickOneValue(key, taxYear)).toMap)
    }

  def retrieveCitizenIncome(requestDetails: RequestDetails, correlationId: String)(implicit hc: HeaderCarrier): Future[DesResponse] =
    desConnector.retrieveCitizenIncome(
      requestDetails.nino,
      RequestDetails.toMatchingRequest(requestDetails),
      correlationId
    ) map {
      case desSuccess: DesSuccessResponse =>
        if (desSuccess.taxYears.isDefined)
          DesFilteredSuccessResponse(desSuccess.matchPattern, pickAll(requestDetails.filterFields, desSuccess))
        else
          DesSuccessResponse(desSuccess.matchPattern, None)
      case failure: DesResponse => failure
    }

}
