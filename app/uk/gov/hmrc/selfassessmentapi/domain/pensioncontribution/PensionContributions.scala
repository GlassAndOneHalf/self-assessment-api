/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentapi.domain.pensioncontribution

import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import uk.gov.hmrc.selfassessmentapi.domain.{PositiveMonetaryFieldDescription, TaxYearPropertyType}

case object PensionContributions extends TaxYearPropertyType {
  override val name: String = "pensionContributions"
  override val example: JsValue = toJson(PensionContribution.example())

  override def description(action: String): String = s"$action a pensionContributions"

  override val title: String = "Sample pension contributions"

  override val fieldDescriptions = Seq(
    PositiveMonetaryFieldDescription(name, "ukRegisteredPension", "Payments to registered pension schemes where basic rate tax relief will be claimed by your pension provider", optional = true),
    PositiveMonetaryFieldDescription(name, "retirementAnnuity", "Payments to a retirement annuity contract where basic rate tax relief will not be claimed by your provider", optional = true),
    PositiveMonetaryFieldDescription(name, "employerScheme", "Payments to your employer’s scheme which were not deducted from your pay before tax", optional = true),
    PositiveMonetaryFieldDescription(name, "overseasPension", "Payments to an overseas pension scheme, which is not UK-registered, which are eligible for tax relief and were not deducted from your pay before tax", optional = true)
  )
}
