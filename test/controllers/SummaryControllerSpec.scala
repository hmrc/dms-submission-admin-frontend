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
import models.{DailySummaryV2, ErrorSummary, SummaryResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
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
import views.html.SummaryView

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SummaryControllerSpec
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
    Mockito.reset[Any](
      mockDmsSubmissionConnector,
      mockStubBehaviour
    )
    super.beforeEach()
  }

  "dailySummaries" - {

    "must return OK and the view when the server returns some data" in {

      val request =
        FakeRequest(GET, routes.SummaryController.onPageLoad(serviceName).url)
          .withSession("authToken" -> "Token some-token")

      val summaryResponse = SummaryResponse(
        errors = ErrorSummary(1, 2),
        summaries = List(DailySummaryV2(LocalDate.now, 1, 2, 3))
      )

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("READ"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.successful(Username("username")))
      when(mockDmsSubmissionConnector.summary(eqTo(serviceName))(any())).thenReturn(Future.successful(summaryResponse))

      val result = route(app, request).value
      val view = app.injector.instanceOf[SummaryView]

      status(result) mustEqual OK
      contentAsString(result) mustEqual view(serviceName, summaryResponse)(request, implicitly).toString

      verify(mockDmsSubmissionConnector, times(1)).summary(eqTo(serviceName))(any())
    }

    "must redirect to login when the user is not authenticated" in {

      val request = FakeRequest(GET, routes.SummaryController.onPageLoad(serviceName).url) // No authToken in session

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual s"/internal-auth-frontend/sign-in?continue_url=%2Fdms-submission-admin-frontend%2F$serviceName%2Fsubmissions%2Fsummary"

      verify(mockDmsSubmissionConnector, times(0)).summary(any())(any())
    }

    "must fail when the user is not authorised" in {

      val predicate = Permission(Resource(ResourceType("dms-submission"), ResourceLocation(serviceName)), IAAction("READ"))
      when(mockStubBehaviour.stubAuth(eqTo(Some(predicate)), eqTo(Retrieval.username))).thenReturn(Future.failed(new Exception("foo")))

      val request =
        FakeRequest(GET, routes.SummaryController.onPageLoad(serviceName).url)
          .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue

      verify(mockDmsSubmissionConnector, times(0)).summary(any())(any())
    }
  }
}
