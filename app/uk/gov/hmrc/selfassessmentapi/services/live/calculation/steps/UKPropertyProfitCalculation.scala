package uk.gov.hmrc.selfassessmentapi.services.live.calculation.steps
import uk.gov.hmrc.selfassessmentapi.repositories.domain.MongoLiability

/**
  * Created by JacobTaylorHindle on 09/08/2016.
  */
object UKPropertyProfitCalculation extends CalculationStep {
  override def run(selfAssessment: SelfAssessment, liability: MongoLiability): MongoLiability = {
    liability
  }
}
