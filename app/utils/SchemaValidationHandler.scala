/*
 * Copyright 2019 HM Revenue & Customs
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

import com.eclipsesource.schema.{SchemaType, SchemaValidator}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import scala.io.Source

trait SchemaValidationHandler {

  def schemaValidationHandler(jsonToValidate: JsValue): Either[JsError, JsSuccess[JsValue]] = {

    val validator = new SchemaValidator()

    val schema: JsValue = {
      val resource = getClass.getResourceAsStream("/schemas/real-time-income-information-post-schema.json")
      Json.parse(Source.fromInputStream(resource).mkString)
    }

    if (validator.validate(Json.fromJson[SchemaType](schema).get)(jsonToValidate).isSuccess)
      Right(JsSuccess(jsonToValidate))
    else
      Left(JsError("Does not validate against any schema"))
  }
}
