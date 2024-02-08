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
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, ResourceType, Retrieval}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IndexController @Inject()(
                                 val controllerComponents: MessagesControllerComponents,
                                 view: IndexView,
                                 dmsSubmissionConnector: DmsSubmissionConnector,
                                 auth: FrontendAuthComponents
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    auth.authenticatedAction(
      continueUrl = routes.IndexController.onPageLoad,
      retrieval = Retrieval.username ~ Retrieval.locations(Some(ResourceType("dms-submission")))
    ).async { implicit request =>
      dmsSubmissionConnector.listServices.map { services =>
        val authorisedServices = request.retrieval.b.map(_.resourceLocation.value)
        val activeAuthorisedServices = authorisedServices.intersect(services)
        val unauthorisedServices = services.diff(activeAuthorisedServices)
        Ok(view(activeAuthorisedServices, unauthorisedServices))
      }
  }
}
