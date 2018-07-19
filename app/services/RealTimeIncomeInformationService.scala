/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class RealTimeIncomeInformationService @Inject()(val desConnector: DesConnector) {

  def pickOneValue(key: String, taxYear: JsValue): (String, JsValue) = {
    taxYear.transform((__ \\ key).json.pick[JsValue]).asOpt match {
      case Some(x) => key -> x
      case None => key -> JsString("undefined")
    }
  }

  def pickAll(keys: List[String], desSuccessResponse: DesSuccessResponse): JsValue = {
    Json.toJson(Map("taxYears" ->
      desSuccessResponse.taxYears.getOrElse(Nil).map(
        taxYear => keys.map(
          key => pickOneValue(key, taxYear)).toMap)))
  }

  def retrieveCitizenIncome(nino: Nino, requestDetails: RequestDetails)(implicit hc: HeaderCarrier) : Future[DesResponse] = {
    desConnector.retrieveCitizenIncome(nino, RequestDetails.toMatchingRequest(requestDetails))(hc) map {
      case desSuccess: DesSuccessResponse => if(desSuccess.matchPattern > 0) {
        DesFilteredSuccessResponse(pickAll(requestDetails.filterFields, desSuccess))
      } else {
        DesSuccessResponse(0, None)
      }
      case failure: DesResponse => failure
    }
  }
}