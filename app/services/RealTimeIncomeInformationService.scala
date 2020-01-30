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

package services

import com.google.inject.{Inject, Singleton}
import connectors.DesConnector
import models.RequestDetails
import models.response._
import play.api.libs.json.{JsValue, Json, _}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class RealTimeIncomeInformationService @Inject()(val desConnector: DesConnector) {

  def pickOneValue(key: String, taxYear: JsValue): Option[(String, JsValue)] = {
    taxYear.transform((__ \\ key).json.pick[JsValue]).asOpt match {
      case Some(x) => Some(key -> x)
      case None => None
    }
  }

  def pickAll(keys: List[String], desSuccessResponse: DesSuccessResponse): List[JsValue] = {
      desSuccessResponse.taxYears.getOrElse(Nil).map(
        taxYear => Json.toJson(keys.flatMap(key => pickOneValue(key, taxYear)).toMap))
  }

  def retrieveCitizenIncome(requestDetails: RequestDetails, correlationId: String)(implicit hc: HeaderCarrier) : Future[DesResponse] = {
    desConnector.retrieveCitizenIncome(requestDetails.nino, RequestDetails.toMatchingRequest(requestDetails), correlationId)(hc) map {
      case desSuccess: DesSuccessResponse => if(desSuccess.taxYears.isDefined) {
        DesFilteredSuccessResponse(desSuccess.matchPattern, pickAll(requestDetails.filterFields, desSuccess))
      } else {
        DesSuccessResponse(desSuccess.matchPattern, None)
      }
      case failure: DesResponse => failure
    }
  }
}