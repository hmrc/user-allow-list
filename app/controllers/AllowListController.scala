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

import models.CheckRequest
import play.api.mvc.{Action, ControllerComponents}
import repositories.AllowListRepository
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AllowListController @Inject()(
                                     override val controllerComponents: ControllerComponents,
                                     auth: BackendAuthComponents,
                                     repository: AllowListRepository
                                   )(implicit ec: ExecutionContext) extends BackendBaseController {

  private val permission = Predicate.Permission(
    resource = Resource(
      ResourceType("user-allow-list"),
      ResourceLocation("check")
    ),
    action = IAAction("READ")
  )

  private val authorised = auth.authorizedAction(permission, Retrieval.username)

  def check(feature: String): Action[CheckRequest] = authorised.compose(Action(parse.json[CheckRequest])).async {
    implicit request =>
      repository
        .check(request.retrieval.value, feature, request.body.value)
        .map {
          case true => Ok
          case false => NotFound
        }
  }
}
