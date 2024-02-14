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
import forms.ConfirmRetryTimeoutFormProvider
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ConfirmRetryTimeoutView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmRetryTimeoutController @Inject()(
                                               val controllerComponents: MessagesControllerComponents,
                                               auth: FrontendAuthComponents,
                                               view: ConfirmRetryTimeoutView,
                                               connector: DmsSubmissionConnector,
                                               formProvider: ConfirmRetryTimeoutFormProvider
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  private val write = IAAction("WRITE")

  private val authorised = (service: String, action: IAAction) =>
    auth.authorizedAction(
      continueUrl = routes.ConfirmRetryTimeoutController.onPageLoad(service),
      predicate = Permission(
        Resource(
          ResourceType("dms-submission"),
          ResourceLocation(service)
        ),
        action
      ),
      retrieval = Retrieval.username
    )

  def onPageLoad(service: String): Action[AnyContent] = authorised(service, write) { implicit request =>
    Ok(view(service, formProvider(service)))
  }

  def onSubmit(service: String): Action[AnyContent] = authorised(service, write).async { implicit request =>
    formProvider(service).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(service, formWithErrors))),
      value =>
        if (value) {
          connector.retryTimeouts(service).map { _ =>
            Redirect(routes.ConfirmRetryTimeoutController.onPageLoad(service))
              .flashing("confirm-retry-timeouts" -> "success")
          }
        } else {
          Future.successful(Redirect(routes.SummaryController.onPageLoad(service)))
        }
    )
  }
}