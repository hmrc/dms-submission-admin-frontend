package models

import play.api.libs.json.{Json, OFormat}

final case class DailySummaryResponse(summaries: List[DailySummary])

object DailySummaryResponse {

  implicit lazy val format: OFormat[DailySummaryResponse] = Json.format
}
