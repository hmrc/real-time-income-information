package controllers

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import services.{AuditService, RealTimeIncomeInformationService}
import uk.gov.hmrc.play.test.UnitSpec
import play.api.inject.bind
import uk.gov.hmrc.auth.core.AuthConnector
import utils.FakeAuthConnector

class RealTimeIncomeInformationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with MockitoSugar {

  val mockRtiiService = mock[RealTimeIncomeInformationService]
  val mockAuditService = mock[AuditService]

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[RealTimeIncomeInformationService].toInstance(mockRtiiService),
        bind[AuditService].toInstance(mockAuditService),
        bind[AuthConnector].toInstance(FakeAuthConnector))
      .build()
  }

  "RealTimeIncomeInformationController" should {
    "Return 200 provided a valid request" when {
      "the service returns a successfully filtered response" in  {

      }

      "the service returns a successful no match with a match pattern of 0" in {

      }

      "the service returns a successful no match with match pattern greater than 0" in {

      }
    }

    "Return 400" when {
      "the request contains an unexpected matching field" in {

      }


      "the request contains an unexpected filter field" in {

      }

      "the filter fields array is empty" in {

      }

      "the filter fields array contains duplicate fields" in {

      }

      "the filter fields array contains an empty string field" in {

      }

      "the service returns a single error response" in {

      }

      "the service returns multiple error responses" in {

      }

      "the correlationId is invalid" in {

      }

      "the toDate is before fromDate" in {

      }

      "the toDate is equal to fromDate" in {

      }

      "a date is in the wrong format" in {

      }
      "either fromDate or toDate is not defined in the request" in {

      }

      "the nino is invalid" in {

      }
    }

    "Return 403 (FORBIDDEN)" when {
      "A non privileged application attempts to call the endpoint" in {

      }
    }

    "Return 404 (NOT_FOUND)" when {
      "The remote endpoint has indicated that there is no data for the Nino" in {

      }

    }

    "Return 500 (SERVER_ERROR)" when {
      "DES is currently experiencing problems that require live service intervention." in {

      }
    }

    "Return 503 (SERVICE_UNAVAILABLE)" when {
      "Dependent systems are currently not responding" in {

      }

      "DesConnector has thrown an Exception" in {

      }
    }

    "Return INTERNAL_SERVER_ERROR" when {
      "DES has given an unexpected response" in {

      }

      "DES has given a failure code and reason that do not match schema" in {

      }
    }
  }
}
