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

import com.google.inject.{Inject, Singleton}
import play.api.{ConfigLoader, Configuration}

@Singleton
class APIConfig @Inject()(configuration: Configuration) {
  private val apiContextConfigKey = "api.context"
  private val apiWhitelistedServicesConfigKey = "api.access.whitelistedApplicationIds"
  private val apiAccessTypeKey = "api.access.type"
  private val privateAccessKey = "PRIVATE"

  lazy val apiContext: String = getOptApiConf[String](apiContextConfigKey).getOrElse(throw apiConfigException(apiContextConfigKey))
  lazy val apiTypeAccess: String = getOptApiConf[String](apiAccessTypeKey).getOrElse(privateAccessKey)
  lazy val apiWhiteList: Option[Seq[String]] = if(apiTypeAccess == privateAccessKey) getOptApiConf[Seq[String]](apiWhitelistedServicesConfigKey) else None

  private def apiConfigException(key: String) = new IllegalStateException(s"$key is not configured")

  private def getOptApiConf[A](key: String)(implicit configLoader: ConfigLoader[A]): Option[A] = configuration.getOptional[A](key)

}
