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

package config

import com.google.inject.Inject
import javax.inject.Singleton
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class ApplicationConfig @Inject()(sc: ServicesConfig) {
  def baseUrl(serviceName: String): String = sc.baseUrl(serviceName)

  lazy val hodUrl: String = baseUrl("des-hod")
  lazy val environment: String = sc.getConfString("des-hod.env", "local")
  lazy val authorization: String = "Bearer " + sc.getConfString("des-hod.authorizationToken", "local")
}
