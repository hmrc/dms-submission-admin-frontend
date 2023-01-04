/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{Done, ObjectSummary, SubmissionItem, SubmissionItemStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
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
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import views.html.SubmissionView

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionControllerSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  private val clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
  private val serviceName = "service"
  private val id = "id"

  private val item = SubmissionItem(
    id = id,
    owner = serviceName,
    callbackUrl = "callbackUrl",
    status = SubmissionItemStatus.Submitted,
    objectSummary = ObjectSummary(
      location = "file",
      contentLength = 1337L,
      contentMd5 = "hash",
      lastModified = clock.instant().minus(2, ChronoUnit.DAYS)
    ),
    failureReason = None,
    created = clock.instant(),
    lastUpdated = clock.instant(),
    sdesCorrelationId = "sdesCorrelationId"
  )

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
    Mockito.reset[Any](
      mockDmsSubmissionConnector,
      mockStubBehaviour
    )
    super.beforeEach()
  }

  "onPageLoad" - {

    "must return OK and the correct view when the user is authorised and there is a submission for this owner/id" in {

      val request = FakeRequest(GET, routes.SubmissionController.onPageLoad(serviceName, id).url)
        .withSession("authToken" -> "Token some-token")

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("READ"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.successful(Username("username")))
      when(mockDmsSubmissionConnector.get(any(), any())(any())).thenReturn(Future.successful(Some(item)))

      val result = route(app, request).value
      val view = app.injector.instanceOf[SubmissionView]

      status(result) mustEqual OK
      contentAsString(result) mustEqual view(serviceName, item)(request, implicitly).toString
      verify(mockDmsSubmissionConnector).get(eqTo(serviceName), eqTo(id))(any())
    }

    "must return NOT_FOUND when the user is authorised and there are no submissions for this owner/id" in {

      val request = FakeRequest(GET, routes.SubmissionController.onPageLoad(serviceName, id).url)
        .withSession("authToken" -> "Token some-token")

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("READ"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.successful(Username("username")))
      when(mockDmsSubmissionConnector.get(eqTo(serviceName), eqTo(id))(any())).thenReturn(Future.successful(None))

      val result = route(app, request).value

      status(result) mustEqual NOT_FOUND
    }

    "must redirect to login when the user is not authenticated" in {

      val request = FakeRequest(GET, routes.SubmissionController.onPageLoad(serviceName, id).url) // No authToken in session

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual s"/internal-auth-frontend/sign-in?continue_url=%2Fdms-submission-admin-frontend%2F$serviceName%2Fsubmissions%2F$id"
    }

    "must fail when the user is not authorised" in {

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("READ"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.failed(new Exception("foo")))

      val request = FakeRequest(GET, routes.SubmissionController.onPageLoad(serviceName, id).url)
        .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
    }
  }

  "retry" - {

    "must redirect to the submission page flashing information about the retry" in {

      val request = FakeRequest(POST, routes.SubmissionController.retry(serviceName, id).url)
        .withSession("authToken" -> "Token some-token")

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("WRITE"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.successful(Username("username")))
      when(mockDmsSubmissionConnector.retry(any(), any())(any())).thenReturn(Future.successful(Done))

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.SubmissionController.onPageLoad(serviceName, id).url
      flash(result).get("dms-submission-retry").value mustEqual id

      verify(mockDmsSubmissionConnector).retry(eqTo(serviceName), eqTo(id))(any())
    }

    "must fail if the retry fails" in {

      val request = FakeRequest(POST, routes.SubmissionController.retry(serviceName, id).url)
        .withSession("authToken" -> "Token some-token")

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("WRITE"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.successful(Username("username")))
      when(mockDmsSubmissionConnector.retry(any(), any())(any())).thenReturn(Future.failed(new RuntimeException()))

      route(app, request).value.failed.futureValue
    }

    "must fail if the user is unauthenticated" in {

      val request = FakeRequest(POST, routes.SubmissionController.retry(serviceName, id).url) // No auth token
      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("WRITE"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.successful(Username("username")))
      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual s"/internal-auth-frontend/sign-in?continue_url=%2Fdms-submission-admin-frontend%2F$serviceName%2Fsubmissions%2F$id"
    }

    "must fail if the user is unauthorised" in {

      val request = FakeRequest(POST, routes.SubmissionController.retry(serviceName, id).url) // No auth token
        .withSession("authToken" -> "Token some-token")

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("WRITE"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.failed(new RuntimeException()))

      route(app, request).value.failed.futureValue
    }
  }
}
