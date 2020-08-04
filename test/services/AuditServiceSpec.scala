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

import java.time.Instant

import models.RequestDetails
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.BaseSpec

import scala.concurrent.Future

class AuditServiceSpec extends BaseSpec with GuiceOneAppPerSuite with Injecting {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  implicit val hc: HeaderCarrier         = HeaderCarrier()
  val appName                            = "myApp"
  val nino: String                       = generateNino

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("appName" -> appName)
      .overrides(bind[AuditConnector].toInstance(mockAuditConnector))
      .build()

  val auditService: AuditService = inject[AuditService]
  val date: Instant              = Instant.now

  "audit" must {
    "send data event to the AuditConnector" in {
      val dataEventCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      val auditPath                                  = "auditPath"
      val auditType                                  = "auditType"
      val auditData                                  = Map.empty[String, String]
      val expectedDataEvent = DataEvent(
        auditSource = appName,
        auditType = auditType,
        eventId = "eventId",
        tags = hc.toAuditTags(auditType, auditPath),
        detail = hc.toAuditDetails() ++ auditData,
        generatedAt = date
      )
      when(mockAuditConnector.sendEvent(dataEventCaptor.capture())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val result = auditService.audit(auditType, auditPath, auditData)

      await(result) mustBe AuditResult.Success
      val actualDataEvent = dataEventCaptor.getValue
      actualDataEvent.copy(eventId = "eventId", generatedAt = date) mustBe expectedDataEvent
    }
  }

  "rtiiAudit" must {
    "audit correlationID, serviceName and filterFields" in {
      val dataEventCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      val correlationId                              = "correlationId"
      val requestDetails = RequestDetails(
        nino,
        "serviceName",
        "2016-12-31",
        "2017-12-31",
        "Smith",
        None,
        None,
        None,
        None,
        None,
        List("surname", "nationalInsuranceNumber")
      )
      val auditPath = s"/individuals/$correlationId/income"
      val auditType = "ServiceRequestReceived"
      val auditData = Map(
        "correlationId" -> correlationId,
        "serviceName"   -> requestDetails.serviceName,
        "filterFields"  -> requestDetails.filterFields.toString()
      )
      val expectedDataEvent = DataEvent(
        auditSource = appName,
        auditType = auditType,
        eventId = "eventId",
        tags = hc.toAuditTags(auditType, auditPath),
        detail = hc.toAuditDetails() ++ auditData,
        generatedAt = date
      )

      when(mockAuditConnector.sendEvent(dataEventCaptor.capture())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val result = auditService.rtiiAudit(correlationId, requestDetails)

      await(result) mustBe AuditResult.Success
      val actualDataEvent = dataEventCaptor.getValue
      actualDataEvent.copy(eventId = "eventId", generatedAt = date) mustBe expectedDataEvent
    }
  }
}
