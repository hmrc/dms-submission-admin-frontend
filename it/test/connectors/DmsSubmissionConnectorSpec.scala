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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import models.{DailySummary, DailySummaryResponse, ListResult, ListServicesResult, ObjectSummary, SubmissionItem, SubmissionItemStatus, SubmissionSummary}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.{ACCEPTED, NOT_FOUND}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, LocalDate, ZoneOffset}

class DmsSubmissionConnectorSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with WireMockSupport
    with OptionValues {

  private lazy val app: Application =
    GuiceApplicationBuilder()
      .configure("microservice.services.dms-submission.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[DmsSubmissionConnector]

  private val clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)

  "get" - {

    val hc = HeaderCarrier()
    val serviceName = "service-name"
    val id = "id"
    val url = s"/dms-submission/$serviceName/submissions/$id"

    val item = SubmissionItem(
      id = id,
      owner = serviceName,
      callbackUrl = "callbackUrl",
      status = SubmissionItemStatus.Submitted,
      objectSummary = ObjectSummary(
        location = "file",
        contentLength = 1337L,
        contentMd5 = "hash",
        lastModified = clock.instant().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
      ),
      failureReason = None,
      created = clock.instant().truncatedTo(ChronoUnit.SECONDS),
      lastUpdated = clock.instant().truncatedTo(ChronoUnit.SECONDS),
      sdesCorrelationId = "sdesCorrelationId"
    )

    "must return a submission item when the server returns OK and some submission" in {

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(ok(Json.stringify(Json.toJson(item))))
      )

      val result = connector.get(serviceName, id)(hc).futureValue

      result.value mustEqual item
    }

    "must return None when the server returns NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(notFound())
      )

      val result = connector.get(serviceName, id)(hc).futureValue

      result mustBe None
    }

    "must return a failed future when the server returns another status" in {

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(serverError())
      )

      connector.get(serviceName, id)(hc).failed.futureValue
    }
  }

  "list" - {

    val hc = HeaderCarrier()
    val serviceName = "service-name"
    val url = s"/dms-submission/$serviceName/submissions"
    val listResult = ListResult(
      totalCount = 2,
      List(
        SubmissionSummary("id1", "Submitted", None, Instant.now.truncatedTo(ChronoUnit.MILLIS)),
        SubmissionSummary("id2", "Processed", None, Instant.now.truncatedTo(ChronoUnit.MILLIS))
      )
    )

    "must return a list of submissions when the server returns OK and some submissions" in {

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(ok(Json.toJson(listResult).toString))
      )

      val result = connector.list(serviceName)(hc).futureValue

      result mustEqual listResult
    }

    "must add filters to the request when passed" in {

      wireMockServer.stubFor(
        get(urlPathMatching(url))
          .withQueryParam("status", equalTo("completed"))
          .withQueryParam("created", equalTo("2022-02-01"))
          .withQueryParam("limit", equalTo("10"))
          .withQueryParam("offset", equalTo("5"))
          .willReturn(ok(Json.toJson(listResult).toString))
      )

      connector.list(
        serviceName,
        status = Some(SubmissionItemStatus.Completed),
        created = Some(LocalDate.of(2022, 2, 1)),
        limit = Some(10),
        offset = Some(5)
      )(hc).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(serverError())
      )

      connector.list(serviceName)(hc).failed.futureValue
    }
  }

  "dailySummaries" - {

    val hc = HeaderCarrier()
    val serviceName = "service-name"
    val url = s"/dms-submission/$serviceName/submissions/summaries"

    "must return a list of summaries when the server returns some" in {

      val summaries = List(
        DailySummary(LocalDate.of(2022, 1, 2), 1, 2, 3, 4, 5),
        DailySummary(LocalDate.of(2022, 1, 1), 6, 7, 8, 9, 0)
      )

      val json = Json.obj(
        "summaries" -> summaries
      )

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(ok(json.toString))
      )

      val result = connector.dailySummaries(serviceName)(hc).futureValue

      result mustEqual DailySummaryResponse(summaries)
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(serverError())
      )

      connector.dailySummaries(serviceName)(hc).failed.futureValue
    }
  }

  "retry" - {

    val hc = HeaderCarrier()
    val serviceName = "service-name"
    val id = "id"
    val url = s"/dms-submission/$serviceName/submissions/$id/retry"

    "must return successfully when the server responds with ACCEPTED" in {

      wireMockServer.stubFor(
        post(urlMatching(url))
          .willReturn(status(ACCEPTED))
      )

      connector.retry(serviceName, id)(hc).futureValue
    }

    "must fail when the server responds with NOT_FOUND" in {

      wireMockServer.stubFor(
        post(urlMatching(url))
          .willReturn(status(NOT_FOUND))
      )

      connector.retry(serviceName, id)(hc).failed.futureValue
    }

    "must fail when the server responds with SERVER_ERROR" in {

      wireMockServer.stubFor(
        post(urlMatching(url))
          .willReturn(serverError())
      )

      connector.retry(serviceName, id)(hc).failed.futureValue
    }
  }

  "listServices" - {

    val hc = HeaderCarrier()
    val url = "/dms-submission/services"

    "must return successfully when the server responds with OK" in {

      val expected = ListServicesResult(Set("foo", "bar"))

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(ok(Json.stringify(Json.toJson(expected))))
      )

      connector.listServices(hc).futureValue mustEqual expected.services
    }

    "must fail when the server responds with NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(notFound())
      )

      connector.listServices(hc).failed.futureValue
    }

    "must fail when the server responds with SERVER_ERROR" in {

      wireMockServer.stubFor(
        get(urlMatching(url))
          .willReturn(serverError())
      )

      connector.listServices(hc).failed.futureValue
    }
  }
}
