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

package utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

trait WireMockHelper extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>

  protected val desServer: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())
  protected val authServer: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  protected def portConfigKey: String

  protected lazy val desApp: Application =
    new GuiceApplicationBuilder()
      .configure(
        portConfigKey -> desServer.port().toString,
        "auditing.enabled" -> false,
        "metrics.enabled" -> false,
        "auth.enabled" -> true
      )
      .build()

  protected lazy val authApp: Application =
    new GuiceApplicationBuilder()
      .configure(
        portConfigKey -> authServer.port().toString,
        "auditing.enabled" -> false,
        "metrics.enabled" -> false,
        "auth.enabled" -> true
      )
      .build()

  protected lazy val desInjector: Injector = desApp.injector
  protected lazy val authInjector: Injector = authApp.injector

  override def beforeAll(): Unit = {
    desServer.start()
    authServer.start()
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    desServer.resetAll()
    authServer.resetAll()
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    desServer.stop()
    authServer.stop()
  }
}