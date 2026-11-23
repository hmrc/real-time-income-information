/*
 * Copyright 2026 HM Revenue & Customs
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
import models.ApiScope
import play.api.Configuration

class APIConfig @Inject() (configuration: Configuration) {
  val apiAccessType: String = configuration.get[String]("api.access")
  val apiContext: String = configuration.get[String]("api.context")
  val serviceNames: Set[String] = configuration.get[Seq[String]]("api.serviceName").toSet

  private lazy val apiFields: Set[String] = Set(
    "annualAmountOfOccupationalPension",
    "apiAvailableTimestamp",
    "bacsPaymentAmount",
    "benefitsTaxedViaPayroll",
    "benefitsTaxedYeartoDate",
    "dateOfBirth",
    "directorsNIC",
    "earningsPeriodsCovered",
    "employeePensionContributions",
    "employeePensionContributionsNotPaid",
    "employeesContributionsInPeriod",
    "employeesContributionsYearToDate",
    "employerName1",
    "employerName2",
    "employerPayeRef",
    "employerTradeName1",
    "employerTradeName2",
    "employmentEndDate",
    "employmentStartDate",
    "flexibleDrawdownNonTaxablePayment",
    "flexibleDrawdownTaxablePayment",
    "flexiblyAccessingPensionRights",
    "forename",
    "gender",
    "grossEarningsForNICs",
    "grossEarningsForNICsYearToDate",
    "hmrcOfficeNumber",
    "hmrcReceiptTimestamp",
    "initials",
    "irregularPayment",
    "itemsSubjectToClass1NIC",
    "latePayeReportingReason",
    "monthlyPeriodNumber",
    "multipleIPSIndicator",
    "nationalInsuranceNumber",
    "niLetter",
    "numberOfNormalHoursWorked",
    "occPenIndicator",
    "onStrikeWithinPayPeriod",
    "partnerForenameOrGivenName",
    "partnerInitials",
    "partnerNINO",
    "partnerSecondForename",
    "partnerSurnameOrFamilyName",
    "payAfterStatutoryDeductions",
    "payFrequency",
    "paymentAfterLeaving",
    "paymentConfidenceStatus",
    "paymentDate",
    "paymentNoLongerValid",
    "paymentsNotSubjectToTaxOrNICS",
    "payrollId",
    "pensionContributionsNotPaidYeartoDate",
    "pensionContributionsYearToDate",
    "pensionsDeathBenefit",
    "rtiReceivedDate",
    "schemeExemptFromElectronicFiling",
    "secondForename",
    "seriousillHealthLumpSum",
    "sharedParentalPayYearToDate",
    "statutoryMaternityPayYeartoDate",
    "statutoryPaternityPayYearToDate",
    "surname",
    "taxCode",
    "taxCodeBasisisNonCumulative",
    "taxDeductedOrRefunded",
    "taxRegime",
    "taxWeekNumberOfAppointmentOfDirector",
    "taxYear",
    "taxYearIndicator",
    "taxablePayInPeriod",
    "taxablePayToDate",
    "totalEmployerNIContributionsInPeriod",
    "totalTaxToDate",
    "trivialCommutationPaymentTypeA",
    "trivialCommutationPaymentTypeB",
    "trivialCommutationPaymentTypeC",
    "uniqueEmploymentSequenceNumber",
    "uniquePaymentId",
    "unpaidAbsencePayPeriod",
    "valueOfDeductions",
    "valueofStatutoryAdoptionPayYearToDate",
    "weeklyPeriodNumber"
  )

  private lazy val limitedApiFields : Set[String] = Set("employmentEndDate", "payFrequency", "paymentDate", "taxablePayInPeriod")

  private lazy val apiScopes: List[ApiScope] = List(
    ApiScope("filter:real-time-income-information-full", apiFields),
    ApiScope("filter:real-time-income-information-sg", limitedApiFields),
    ApiScope("filter:real-time-income-information-csp", limitedApiFields)
  )
  

  def findScope(scopeName: String): Option[ApiScope] = apiScopes.find(_.name == scopeName)
  
}
