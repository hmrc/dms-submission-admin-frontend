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

package models

import play.api.mvc.QueryStringBindable

sealed trait SubmissionItemStatus extends Product with Serializable

object SubmissionItemStatus {

  case object Submitted extends SubmissionItemStatus
  case object Forwarded extends SubmissionItemStatus
  case object Processed extends SubmissionItemStatus
  case object Failed extends SubmissionItemStatus
  case object Completed extends SubmissionItemStatus

  implicit lazy val queryStringBindable: QueryStringBindable[SubmissionItemStatus] =
    new QueryStringBindable.Parsing(
      _.toLowerCase match {
        case "submitted" => Submitted
        case "forwarded" => Forwarded
        case "processed" => Processed
        case "failed"    => Failed
        case "completed" => Completed
      },
      {
        case Submitted => "submitted"
        case Forwarded => "forwarded"
        case Processed => "processed"
        case Failed    => "failed"
        case Completed => "completed"
      },
      (key, _) => s"$key: invalid status"
    )
}