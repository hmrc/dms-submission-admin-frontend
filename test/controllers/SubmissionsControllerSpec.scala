/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.SubmissionsView

class SubmissionsControllerSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with OptionValues {

  private val app = GuiceApplicationBuilder().build()

  private implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  "onPageLoad" - {

    "must return OK and the correct view" in {

      val request = FakeRequest(GET, routes.SubmissionsController.onPageLoad("service").url)

      val result = route(app, request).value
      val view = app.injector.instanceOf[SubmissionsView]

      status(result) mustEqual OK
      contentAsString(result) mustEqual view("service")(request, implicitly).toString
    }
  }
}
