/*
 * Copyright 2024 HM Revenue & Customs
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

package test_utils

import play.api.libs.json.{JsValue, Json}

import scala.io.Source


trait ResourceProvider {

  def dwpRequest(nino: String): JsValue = getRequest("dwp-request", nino)

  def fullDesResponse(nino: String): JsValue = readJson(s"full-des-response.json", nino)

  def getRequest(fileName: String, nino: String): JsValue = readJson(s"./requests/$fileName.json", nino)

  def getResponse(fileName: String, nino: String): JsValue = readJson(s"./responses/$fileName.json", nino)

  private def readJson(path: String, nino: String): JsValue = {
    val resource = Source.fromResource(path).getLines().mkString.replace("QQ123456C",  nino)
    Json.parse(resource)
  }
}
