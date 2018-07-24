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

package config

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.Play.current
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

trait HodConfig {
  val baseURL: String
  val environment: String
  val authorization: String
}

abstract class BaseConfig(playEnv: Environment) extends ServicesConfig {
  override val mode = playEnv.mode
}

@Singleton
class DesConfig @Inject() (val runModeConfiguration: Configuration, playEnv: Environment) extends BaseConfig(playEnv) with HodConfig {
  lazy val baseURL: String = baseUrl("des-hod")
  lazy val environment: String = runModeConfiguration.getString(s"$rootServices.des-hod.env").getOrElse("local")
  lazy val authorization: String = "Bearer " + runModeConfiguration.getString(s"$rootServices.des-hod.authorizationToken").getOrElse("local")
}


