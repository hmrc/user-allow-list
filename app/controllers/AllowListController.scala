package controllers

import models.CheckRequest
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class AllowListController @Inject()(override val controllerComponents: ControllerComponents) extends BackendBaseController {

  def check(feature: String): Action[CheckRequest] = Action(parse.json[CheckRequest]) {
    implicit request =>
      Ok
  }

}
