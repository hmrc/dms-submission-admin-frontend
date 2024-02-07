/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.DmsSubmissionConnector
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, IAAction, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SubmissionView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmissionController @Inject()(
                                      val controllerComponents: MessagesControllerComponents,
                                      connector: DmsSubmissionConnector,
                                      view: SubmissionView,
                                      auth: FrontendAuthComponents
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private def authorised(service: String, id: String, action: IAAction) =
    auth.authorizedAction(
      continueUrl = routes.SubmissionController.onPageLoad(service, id),
      predicate = Permission(
        Resource(
          ResourceType("dms-submission"),
          ResourceLocation(service)
        ),
        action
      ),
      retrieval = Retrieval.username
    )

  def onPageLoad(service: String, id: String): Action[AnyContent] =
    authorised(service, id, IAAction("READ")).async { implicit request =>
      connector.get(service, id).map {
        _.map(item => Ok(view(service, item)))
          .getOrElse(NotFound)
      }
    }

  def retry(service: String, id: String): Action[AnyContent] =
    authorised(service, id, IAAction("WRITE")).async { implicit request =>
      connector.retry(service, id).map { _ =>
        Redirect(routes.SubmissionController.onPageLoad(service, id))
          .flashing("dms-submission-retry" -> id)
      }
    }
}
