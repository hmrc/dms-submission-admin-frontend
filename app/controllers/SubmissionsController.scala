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
import models.{ListResult, SubmissionItemStatus}
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SubmissionsView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubmissionsController @Inject()(
                                       val controllerComponents: MessagesControllerComponents,
                                       configuration: Configuration,
                                       auth: FrontendAuthComponents,
                                       view: SubmissionsView,
                                       connector: DmsSubmissionConnector
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val limit: Int = configuration.get[Int]("submissions.limit")

  private val read = IAAction("READ")

  private val authorised = (service: String, action: IAAction) =>
    auth.authorizedAction(
      continueUrl = routes.SubmissionsController.onPageLoad(service),
      predicate = Permission(
        Resource(
          ResourceType("dms-submission"),
          ResourceLocation(service)
        ),
        action
      ),
      retrieval = Retrieval.username
    )

  def onPageLoad(
                  service: String,
                  status: Option[SubmissionItemStatus],
                  created: Option[LocalDate],
                  offset: Option[Int]
                ): Action[AnyContent] = authorised(service, read).async { implicit request =>
    connector.list(service, status, created, Some(limit), offset).map { listResult =>
      Ok(view(service, listResult.summaries, status, created, limit, offset.getOrElse(0), listResult.totalCount))
    }
  }
}
