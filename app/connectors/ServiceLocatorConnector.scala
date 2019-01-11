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

package connectors

import com.google.inject.Inject
import config.ApplicationConfig
import play.api.Logger
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

case class Registration(serviceName: String, serviceUrl: String, metadata: Option[Map[String, String]] = None)

object Registration {
  implicit val regFormat = Json.format[Registration]
}

class ServiceLocatorConnector @Inject()(http: HttpClient, configuration: ApplicationConfig) {

  private lazy val appName: String = configuration.getString("appName")
  private lazy val appUrl: String = configuration.getString("appUrl")
  private lazy val serviceUrl: String = configuration.baseUrl("service-locator")

  val handlerOK: () => Unit = () => Logger.info("Service is registered on the service locator")
  val handlerError: Throwable => Unit = e => Logger.error("Service could not register on the service locator", e)
  val metadata: Option[Map[String, String]] = Some(Map("third-party-api" -> "true"))

  def register(): Future[Boolean] = {
    implicit val hc: HeaderCarrier = new HeaderCarrier

    val registration = Registration(appName, appUrl, metadata)
    http.POST(s"$serviceUrl/registration", registration, Seq(HeaderNames.CONTENT_TYPE -> ContentTypes.JSON)) map {
      _ =>
        handlerOK()
        true
    } recover {
      case e: Throwable =>
        handlerError(e)
        false
    }
  }
}
