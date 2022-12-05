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

package connectors

import config.Service
import models.{DailySummaryResponse, Done, SubmissionItem, SubmissionItemStatus, SubmissionSummary, javaLocalDateQueryStringBindable}
import play.api.Configuration
import play.api.http.Status.ACCEPTED
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.net.URL

@Singleton
class DmsSubmissionConnector @Inject()(
                                        httpClient: HttpClientV2,
                                        configuration: Configuration
                                      )(implicit ec: ExecutionContext) {

  private val dmsSubmissionService: Service = configuration.get[Service]("microservice.services.dms-submission")

  def get(serviceName: String, id: String)(implicit hc: HeaderCarrier): Future[Option[SubmissionItem]] =
    httpClient
      .get(url"${dmsSubmissionService.baseUrl}/dms-submission/$serviceName/submissions/$id")
      .execute[Option[SubmissionItem]]

  def list(
            serviceName: String,
            status: Option[SubmissionItemStatus] = None,
            created: Option[LocalDate] = None,
            limit: Option[Int] = None,
            offset: Option[Int] = None
          )(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] = {

    val localDateBinder: QueryStringBindable[LocalDate] = implicitly
    val statusBinder: QueryStringBindable[SubmissionItemStatus] = implicitly
    val intBinder: QueryStringBindable[Int] = implicitly

    val params = List(
      status.map(statusBinder.unbind("status", _)),
      created.map(localDateBinder.unbind("created", _)),
      limit.map(intBinder.unbind("limit", _)),
      offset.map(intBinder.unbind("offset", _))
    ).flatten

    val query = if (params.isEmpty) "" else {
      params.mkString("?", "&", "")
    }

    httpClient
      .get(new URL(s"${dmsSubmissionService.baseUrl}/dms-submission/$serviceName/submissions$query"))
      .execute[Seq[SubmissionSummary]]
  }

  def dailySummaries(serviceName: String)(implicit hc: HeaderCarrier): Future[DailySummaryResponse] =
    httpClient
      .get(url"${dmsSubmissionService.baseUrl}/dms-submission/$serviceName/submissions/summaries")
      .execute[DailySummaryResponse]

  def retry(serviceName: String, id: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .post(url"${dmsSubmissionService.baseUrl}/dms-submission/$serviceName/submissions/$id/retry")
      .execute
      .flatMap { response =>
        if (response.status == ACCEPTED) {
          Future.successful(Done)
        } else {
          Future.failed(UpstreamErrorResponse("Unexpected response to retry request", response.status))
        }
      }
}
