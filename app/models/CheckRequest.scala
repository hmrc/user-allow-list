package models

import play.api.libs.json.{Json, OFormat}

final case class CheckRequest(value: String)

object CheckRequest {

  implicit lazy val format: OFormat[CheckRequest] = Json.format
}
