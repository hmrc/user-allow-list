/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class Summary(feature: String, count: Int)

object Summary {

  private lazy val mongoReads: Reads[Summary] = (
    (__ \ "_id").read[String] and
    (__ \ "count").read[Int]
  )(Summary.apply _)

  private lazy val mongoWrites: OWrites[Summary] = (
    (__ \ "_id").write[String] and
    (__ \ "count").write[Int]
  )(unlift(Summary.unapply))

  lazy val mongoFormat: OFormat[Summary] = OFormat(mongoReads, mongoWrites)

  implicit lazy val format: OFormat[Summary] = Json.format
}

