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

package models

import models.SubmissionItem.FailureType
import play.api.libs.json.{Format, JsString, Json, OFormat, Reads, Writes, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant


final case class SubmissionItem(
                                 id: String,
                                 owner: String,
                                 callbackUrl: String,
                                 status: SubmissionItemStatus,
                                 objectSummary: ObjectSummary,
                                 failureType: Option[FailureType],
                                 failureReason: Option[String],
                                 sdesCorrelationId: String,
                                 created: Instant,
                                 lastUpdated: Instant,
                                 lockedAt: Option[Instant] = None
                               )

object SubmissionItem extends MongoJavatimeFormats.Implicits {

  sealed trait FailureType extends Product with Serializable

  object FailureType {

    case object Timeout extends FailureType
    case object Sdes extends FailureType

    lazy val reads: Reads[FailureType] =
      __.read[String].flatMap {
        case "timeout" => Reads.pure(Timeout)
        case "sdes" => Reads.pure(Sdes)
        case other => Reads.failed(s"invalid failure type: $other")
      }

    lazy val writes: Writes[FailureType] =
      Writes {
        case Timeout => JsString("timeout")
        case Sdes => JsString("sdes")
      }

    implicit lazy val format: Format[FailureType] =
      Format(reads, writes)
  }

  implicit lazy val format: OFormat[SubmissionItem] = Json.format
}
