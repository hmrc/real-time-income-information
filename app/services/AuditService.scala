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

package services

import com.google.inject.{Inject, Singleton}
import models.RequestDetails
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuditService @Inject()(configuration: Configuration, connector: AuditConnector) extends AppName {

  def audit(auditType: String, path: String, auditData: Map[String, String])(implicit hc:HeaderCarrier): Future[AuditResult] = {
    val event = DataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = hc.toAuditTags(auditType, path),
      detail = hc.toAuditDetails() ++ auditData
    )

    connector.sendEvent(event)
  }

  def rtiiAudit(correlationId: String, body: RequestDetails)(implicit hc: HeaderCarrier) = {
    audit("ServiceRequestReceived", s"/individuals/$correlationId/income",
      auditData = Map("correlationId" -> correlationId, "serviceName" -> body.serviceName, "filterFields" -> body.filterFields.toString()))
  }

  override protected def appNameConfiguration: Configuration = configuration
}

