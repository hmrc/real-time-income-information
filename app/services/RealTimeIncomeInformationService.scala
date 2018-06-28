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

import models.response.DesSuccessResponse
import play.api.libs.json.{JsValue, Json, _}

class RealTimeIncomeInformationService {

  def pickOneValue(key: String, desResponse: DesSuccessResponse): Map[String, JsValue] = {
    val json = Json.toJson(desResponse.response)
    val jsonTransformer = (__ \\ key).json.pick[JsValue]
    json.transform(jsonTransformer).asOpt match {
      case Some(x) => Map(key -> x)
      case None => Map(key -> JsString("undefined"))
    }
  }

  def pickAll(keys: List[String],desResponse: DesSuccessResponse): JsObject = {
    JsObject(keys.flatMap((key: String) => pickOneValue(key, desResponse)))
  }

}
