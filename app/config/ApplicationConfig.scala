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

package config

import com.google.inject.Inject

import javax.inject.Singleton
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.Duration
import scala.concurrent.duration._

@Singleton
class ApplicationConfig @Inject() (sc: ServicesConfig, configuration: Configuration) {
  val schemaResourcePath: String = configuration.get[String]("schemaResourcePath")
  val hodUrl: String             = sc.baseUrl("des-hod")
  val environment: String        = sc.getConfString("des-hod.env", "local")
  val authorization: String      = "Bearer " + sc.getConfString("des-hod.authorizationToken", "local")
  val authBaseUrl: String        = sc.baseUrl("auth")
  val cacheExpireAfter: Duration = sc.getConfDuration("mongodb.cacheExpireAfter", 15.minutes)
}
