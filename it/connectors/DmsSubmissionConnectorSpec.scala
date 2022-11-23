package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, serverError, urlMatching}
import models.SubmissionSummary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import util.WireMockHelper

import java.time.Instant

class DmsSubmissionConnectorSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with WireMockHelper {

  private lazy val app: Application =
    GuiceApplicationBuilder()
      .configure("microservice.services.dms-submission.port" -> server.port())
      .build()

  private lazy val connector = app.injector.instanceOf[DmsSubmissionConnector]

  "list" - {

    val hc = HeaderCarrier()
    val serviceName = "service-name"
    val url = s"/dms-submission/$serviceName/submissions"

    "must return a list of submissions when the server returns OK and some submissions" in {

      val submissions = List(
        SubmissionSummary("id1", "Submitted", None, Instant.now),
        SubmissionSummary("id2", "Processed", None, Instant.now)
      )

      server.stubFor(
        get(urlMatching(url))
          .willReturn(ok(Json.toJson(submissions).toString))
      )

      val result = connector.list(serviceName)(hc).futureValue

      result mustEqual submissions
    }

    "must return an empty list when the server returns OK and no submissions" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(ok("[]"))
      )

      val result = connector.list(serviceName)(hc).futureValue

      result mustBe empty
    }

    "must return a failed future when the server returns an error" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(serverError())
      )

      connector.list(serviceName)(hc).failed.futureValue
    }
  }
}
