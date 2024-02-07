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

import auth.Retrievals.Ops
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.internalauth.client.Retrieval.Username
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client._
import views.html.IndexView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IndexControllerSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  private val mockStubBehaviour = mock[StubBehaviour]
  private val stubFrontendAuthComponents = FrontendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents(), implicitly)

  private val app = GuiceApplicationBuilder()
    .overrides(bind[FrontendAuthComponents].toInstance(stubFrontendAuthComponents))
    .build()

  private implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  override def beforeEach(): Unit = {
    Mockito.reset(mockStubBehaviour)
    super.beforeEach()
  }

  "Index Controller" - {

    "must return OK and the correct view for a GET from an authorised user" in {

      val resource = Resource(ResourceType("dms-submission"), ResourceLocation("foo"))
      val retrieval = Username("username") ~ Set(resource)
      when(mockStubBehaviour.stubAuth[Username ~ Set[Resource]](eqTo(None), any())).thenReturn(Future.successful(retrieval))

      val request =
        FakeRequest(GET, routes.IndexController.onPageLoad.url)
          .withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      val view = app.injector.instanceOf[IndexView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual view(Set(resource))(request, implicitly).toString
    }

    "must redirect to internal-auth-frontend for an unauthenticated user" in {

      val request = FakeRequest(GET, routes.IndexController.onPageLoad.url) // No Authorization token

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual "/internal-auth-frontend/sign-in?continue_url=%2Fdms-submission-admin-frontend"
    }
  }
}
