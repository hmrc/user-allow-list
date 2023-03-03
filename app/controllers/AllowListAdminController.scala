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

package controllers

import models.{CheckRequest, DeleteRequest, SetRequest}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class AllowListAdminController @Inject()(override val controllerComponents: ControllerComponents) extends BackendBaseController {

  def set(service: String, feature: String): Action[SetRequest] = Action(parse.json[SetRequest]) {
    implicit request =>
      Ok
  }

  def delete(service: String, feature: String): Action[DeleteRequest] = Action(parse.json[DeleteRequest]) {
    implicit request =>
      Ok
  }

  def check(service: String, feature: String): Action[CheckRequest] = Action(parse.json[CheckRequest]) {
    implicit request =>
      Ok
  }

  def count(service: String, feature: String): Action[AnyContent] = Action {
    implicit request =>
      Ok
  }

  def summary(service: String): Action[AnyContent] = Action {
    implicit request =>
      Ok
  }

  def clear(service: String, feature: String): Action[AnyContent] = Action {
    implicit request =>
      Ok
  }
}
