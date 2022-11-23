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

import connectors.DmsSubmissionConnector
import models.SubmissionSummary
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
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
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client.Retrieval.Username
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client._
import views.html.SubmissionsView

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionsControllerSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  private val serviceName = "service"

  private val mockDmsSubmissionConnector = mock[DmsSubmissionConnector]
  private val mockStubBehaviour = mock[StubBehaviour]
  private val stubFrontendAuthComponents = FrontendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents(), implicitly)

  private val app = GuiceApplicationBuilder()
    .overrides(
      bind[FrontendAuthComponents].toInstance(stubFrontendAuthComponents),
      bind[DmsSubmissionConnector].toInstance(mockDmsSubmissionConnector)
    )
    .build()

  private implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  override protected def beforeEach(): Unit = {
    Mockito.reset(
      mockDmsSubmissionConnector,
      mockStubBehaviour
    )
    super.beforeEach()
  }

  "onPageLoad" - {

    "must return OK and the correct view when the user is authorised and there are some submissions for this service" in {

      val request =
        FakeRequest(GET, routes.SubmissionsController.onPageLoad(serviceName).url)
          .withSession("authToken" -> "Token some-token")

      val submissions = List(SubmissionSummary("id", "Processed", None, Instant.now))
      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("READ"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.successful(Username("username")))
      when(mockDmsSubmissionConnector.list(eqTo(serviceName))(any())).thenReturn(Future.successful(submissions))

      val result = route(app, request).value
      val view = app.injector.instanceOf[SubmissionsView]

      status(result) mustEqual OK
      contentAsString(result) mustEqual view(serviceName, submissions)(request, implicitly).toString
    }

    "must return OK and the correct view when the user is authorised and there are no submissions for this service" in {

      val request =
        FakeRequest(GET, routes.SubmissionsController.onPageLoad(serviceName).url)
          .withSession("authToken" -> "Token some-token")

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("READ"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.successful(Username("username")))
      when(mockDmsSubmissionConnector.list(eqTo(serviceName))(any())).thenReturn(Future.successful(Nil))

      val result = route(app, request).value
      val view = app.injector.instanceOf[SubmissionsView]

      status(result) mustEqual OK
      contentAsString(result) mustEqual view(serviceName, Nil)(request, implicitly).toString
    }

    "must redirect to login when the user is not authenticated" in {

      val request = FakeRequest(GET, routes.SubmissionsController.onPageLoad(serviceName).url) // No authToken in session

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual s"/internal-auth-frontend/sign-in?continue_url=%2Fdms-submission-admin-frontend%2F$serviceName%2Fsubmissions"
    }

    "must fail when the user is not authorised" in {

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("READ"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.failed(new Exception("foo")))

      val request =
        FakeRequest(GET, routes.SubmissionsController.onPageLoad(serviceName).url)
          .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
    }
  }
}
