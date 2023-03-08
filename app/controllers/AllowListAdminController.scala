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

import models.{CheckRequest, CountResponse, DeleteRequest, SetRequest, SummaryResponse}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.AllowListRepository
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AllowListAdminController @Inject()(
                                          override val controllerComponents: ControllerComponents,
                                          auth: BackendAuthComponents,
                                          repository: AllowListRepository
                                        )(implicit ec: ExecutionContext) extends BackendBaseController {

  private val authorised = (service: String) =>
    auth.authorizedAction(Permission(
      Resource(
        ResourceType("user-allow-list-admin"),
        ResourceLocation(service)
      ),
      IAAction("ADMIN")
    ), Retrieval.username)

  def set(service: String, feature: String): Action[SetRequest] = authorised(service).compose(Action(parse.json[SetRequest])).async {
    implicit request =>
      repository
        .set(service, feature, request.body.values)
        .map(_ => Ok)
  }

  def delete(service: String, feature: String): Action[DeleteRequest] = authorised(service).compose(Action(parse.json[DeleteRequest])).async {
    implicit request =>
      repository
        .remove(service, feature, request.body.values)
        .map(_ => Ok)
  }

  def check(service: String, feature: String): Action[CheckRequest] = authorised(service).compose(Action(parse.json[CheckRequest])).async {
    implicit request =>
      repository
        .check(service, feature, request.body.value)
        .map {
          case true => Ok
          case false => NotFound
        }  }

  def count(service: String, feature: String): Action[AnyContent] = authorised(service).async {
    implicit request =>
      repository
        .count(service, feature)
        .map(count => Ok(Json.toJson(CountResponse(count))))
  }

  def summary(service: String): Action[AnyContent] = authorised(service).async {
    implicit request =>
      repository
        .summary(service)
        .map(summaries => Ok(Json.toJson(SummaryResponse(summaries))))
  }

  def clear(service: String, feature: String): Action[AnyContent] = authorised(service).async {
    implicit request =>
      repository
        .clear(service, feature)
        .map(_ => Ok)
  }
}
