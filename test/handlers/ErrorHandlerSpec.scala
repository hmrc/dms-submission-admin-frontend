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

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.UpstreamErrorResponse

class ErrorHandlerSpec extends AnyFreeSpec with Matchers with ScalaFutures with OptionValues with GuiceOneAppPerSuite {

  private lazy val handler = app.injector.instanceOf[ErrorHandler]

  "must return the correct view when an upstream 403 error is propagated" in {

    val request = FakeRequest()

    val messagesApi = app.injector.instanceOf[MessagesApi]
    implicit val messages: Messages = messagesApi.preferred(request)

    val error = UpstreamErrorResponse("Forbidden", Status.FORBIDDEN)

    val expectedContent = handler.standardErrorTemplate(
      Messages("global.error.forbidden403.title"),
      Messages("global.error.forbidden403.heading"),
      Messages("global.error.forbidden403.message")
    )(request).body

    val result = handler.onServerError(request, error)

    status(result) mustEqual Status.FORBIDDEN
    contentAsString(result) mustEqual expectedContent
  }
}
