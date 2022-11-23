package models

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

final case class DailySummary(
                               date: LocalDate,
                               submitted: Int,
                               forwarded: Int,
                               processed: Int,
                               failed: Int,
                               completed: Int
                             )

object DailySummary {

  implicit lazy val format: OFormat[DailySummary] = Json.format
}
