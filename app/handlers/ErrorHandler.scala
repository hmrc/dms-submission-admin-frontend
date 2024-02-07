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

package handlers

import play.api.http.Status.FORBIDDEN
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.Forbidden
import play.api.mvc.{Request, RequestHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.ErrorTemplate

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject()(
                              val messagesApi: MessagesApi,
                              view: ErrorTemplate
                            ) extends FrontendErrorHandler with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    view(pageTitle, heading, message)

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    exception match {
      case e: UpstreamErrorResponse if e.statusCode == FORBIDDEN =>
        Future.successful(Forbidden(forbiddenErrorTemplate(Request(request, ""))))
      case _ =>
        super.onServerError(request, exception)
    }

  private def forbiddenErrorTemplate(implicit request: Request[_]): Html =
    standardErrorTemplate(
      Messages("global.error.forbidden403.title"),
      Messages("global.error.forbidden403.heading"),
      Messages("global.error.forbidden403.message")
    )
}
