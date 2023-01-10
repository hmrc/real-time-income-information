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

import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

@Singleton
class APIConfig @Inject() (configuration: Configuration) {
  private val apiAccessTypeKey = "api.access.type"
  private val apiContextKey = "api.context"
  private val apiFieldsKey = "api.fields"
  private val apiScopesKey = "api.scopes"
  private val privateAccessKey = "PRIVATE"

  private lazy val apiFields: List[ApiField] = getOptional[List[ApiField]](apiFieldsKey).getOrElse(throw apiConfigException(apiFieldsKey))

  private lazy val apiScopes: List[ApiScope] = getOptional[List[ApiScope]](apiScopesKey).getOrElse(throw apiConfigException(apiScopesKey))

  val serviceNames = getOptional[Seq[String]]("api.serviceName").getOrElse(Seq())

  private def apiConfigException(key: String) = new IllegalStateException(s"$key is not configured")

  private def getOptional[A](key: String)(implicit configLoader: ConfigLoader[A]): Option[A] = configuration.getOptional[A](key)

  private def getField(id: Int): Option[ApiField] = apiFields.find(c => c.id == id)

  implicit val apiFieldsConfigLoader: ConfigLoader[List[ApiField]] = new ConfigLoader[List[ApiField]] {
    override def load(rootConfig: Config, path: String): List[ApiField] = {
      val fieldsConfig = rootConfig.getConfig(apiFieldsKey)
      val fields = fieldsConfig.entrySet.map { entry => {
        ApiField(entry.getKey.toInt, fieldsConfig.getString(entry.getKey))
      }}.toList

      fields
    }
  }

  implicit val apiScopesConfigLoader: ConfigLoader[List[ApiScope]] = new ConfigLoader[List[ApiScope]] {
    override def load(rootConfig: Config, path: String): List[ApiScope] = {

      def generateScopeName(entryKey: String) = {
        entryKey.replace(".fields", "").replace("\"", "")
      }

      val scopesConfig = rootConfig.getConfig(apiScopesKey)

      val scopes = scopesConfig.entrySet.map { entry =>

      val fields = scopesConfig.getIntList(entry.getKey).map { key =>
        val value = getField(key).getOrElse(throw apiConfigException(apiFieldsKey))
        val apiField = ApiField(key, value.name)
        apiField
      }.toList

      val scopeName = generateScopeName(entry.getKey)
      val apiScope = ApiScope(scopeName, fields)
        apiScope
      }.toList
      scopes
    }
  }

  lazy val apiAccessType: String = getOptional[String](apiAccessTypeKey).getOrElse(privateAccessKey)

  lazy val apiContext: String = getOptional[String](apiContextKey).getOrElse(throw apiConfigException(apiContextKey))

  def findScope(scopeName: String): Option[ApiScope] = apiScopes.find(apiScope => apiScope.name == scopeName)
}
