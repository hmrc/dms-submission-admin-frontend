package models

import play.api.libs.json.{Json, OFormat}

final case class ListServicesResult(services: Set[String])

object ListServicesResult {

  implicit lazy val format: OFormat[ListServicesResult] = Json.format
}
