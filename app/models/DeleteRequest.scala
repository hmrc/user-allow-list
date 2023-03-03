package models

import play.api.libs.json.{Json, OFormat}

final case class DeleteRequest(values: Set[String])

object DeleteRequest {

  implicit lazy val format: OFormat[DeleteRequest] = Json.format
}
