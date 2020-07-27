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

import java.io.InputStream

import com.eclipsesource.schema.{SchemaType, SchemaValidator => Validator}
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{fromJson, parse}

import scala.io.Source

@Singleton
class SchemaValidator @Inject()(applicationConfig: ApplicationConfig){

  private val schemaType: SchemaType = {
    val resource: InputStream = getClass.getResourceAsStream(applicationConfig.schemaResourcePath)
    fromJson[SchemaType](parse(Source.fromInputStream(resource).mkString)).get
  }

  private val validator = Validator()

  def validate(jsonToValidate: JsValue): Boolean =
    validator.validate(schemaType)(jsonToValidate).isSuccess

}
