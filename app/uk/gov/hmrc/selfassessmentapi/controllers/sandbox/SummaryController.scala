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

package uk.gov.hmrc.selfassessmentapi.controllers.sandbox

import play.api.mvc.Action
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.selfassessmentapi.config.AppContext
import uk.gov.hmrc.selfassessmentapi.controllers.{BaseController, Links}
import uk.gov.hmrc.selfassessmentapi.domain._
import play.api.mvc.hal._
import play.api.hal.HalLink
import play.api.libs.json.Json._
import scala.concurrent.ExecutionContext.Implicits.global

object SummaryController extends BaseController with Links {

  override lazy val context: String = AppContext.apiGatewayContext

  def handler(sourceType: SourceType, summaryType: SummaryType): SummaryHandler[_] = (sourceType, summaryType) match {
    case (SelfEmploymentsSourceType, SummaryTypes.SelfEmploymentIncomes) => IncomesSummaryHandler
    case (SelfEmploymentsSourceType, SummaryTypes.Expenses) => ExpensesSummaryHandler
    case (SelfEmploymentsSourceType, SummaryTypes.BalancingCharges) => BalancingChargesSummaryHandler
    case (SelfEmploymentsSourceType, SummaryTypes.GoodsAndServicesOwnUse) => GoodsAndServiceOwnUseSummaryHandler
    case (FurnishedHolidayLettingsSourceType, SummaryTypes.PrivateUseAdjustment) => PrivateUseAdjustmentSummaryHandler
    case (FurnishedHolidayLettingsSourceType, SummaryTypes.FurnishedHolidayLettingsIncome) => FurnishedHolidayLettingsIncomeSummaryHandler
    case _ => throw new IllegalArgumentException(s"""Unsupported combination of sourceType "${sourceType.name}" and "${summaryType.name}""")
  }

  def create(saUtr: SaUtr, taxYear: TaxYear, sourceType: SourceType, sourceId: SourceId, summaryType: SummaryType) = Action.async(parse.json) { implicit request =>
    handler(sourceType, summaryType).create(request.body) map {
      case Left(errorResult) =>
        errorResult match {
          case ErrorResult(Some(message), _) => BadRequest(message)
          case ErrorResult(_, Some(errors)) => BadRequest(failedValidationJson(errors))
          case _ => BadRequest
        }
      case Right(id) =>
        Created(halResource(obj(), Seq(HalLink("self",  sourceTypeAndSummaryTypeIdHref(saUtr, taxYear, sourceType, sourceId, summaryType, id)))))
    }
  }

  def read(saUtr: SaUtr, taxYear: TaxYear, sourceType: SourceType, sourceId: SourceId, summaryType: SummaryType, summaryId: SummaryId) = Action.async { implicit request =>
    handler(sourceType, summaryType).findById(summaryId) map {
      case Some(summary) =>
        Ok(halResource(toJson(summary), Seq(HalLink("self",  sourceTypeAndSummaryTypeIdHref(saUtr, taxYear, sourceType, sourceId, summaryType, summaryId)))))
      case None =>
        NotFound
    }
  }

  def update(saUtr: SaUtr, taxYear: TaxYear, sourceType: SourceType, sourceId: SourceId, summaryType: SummaryType, summaryId: SummaryId) = Action.async(parse.json) { implicit request =>
    handler(sourceType, summaryType).update(summaryId, request.body) map {
      case Left(errorResult) =>
        errorResult match {
          case ErrorResult(Some(message), _) => BadRequest(message)
          case ErrorResult(_, Some(errors)) => BadRequest(failedValidationJson(errors))
          case _ => BadRequest
        }
      case Right(id) =>
        Ok(halResource(obj(), Seq(HalLink("self", sourceTypeAndSummaryTypeIdHref(saUtr, taxYear, sourceType, sourceId, summaryType, id)))))
    }
  }


  def delete(saUtr: SaUtr, taxYear: TaxYear, sourceType: SourceType, sourceId: SourceId, summaryType: SummaryType, summaryId: SummaryId) = Action.async { implicit request =>
    handler(sourceType, summaryType).delete(summaryId) map {
      case true =>
        NoContent
      case false =>
        NotFound
    }
  }


  def list(saUtr: SaUtr, taxYear: TaxYear, sourceType: SourceType, sourceId: SourceId, summaryType: SummaryType) = Action.async { implicit request =>
    val svc = handler(sourceType, summaryType)
      svc.find map { summaryIds =>
      val json = toJson(summaryIds.map(id => halResource(obj(),
        Seq(HalLink("self", sourceTypeAndSummaryTypeIdHref(saUtr, taxYear, sourceType, sourceId, summaryType, id))))))

      Ok(halResourceList(svc.listName, json, sourceTypeAndSummaryTypeHref(saUtr, taxYear, sourceType, sourceId, summaryType)))
    }
  }

}
