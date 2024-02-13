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

import play.api.mvc.QueryStringBindable

import java.time.LocalDate
import java.time.format.DateTimeFormatter

package object models {

  private val formatter: DateTimeFormatter =
    DateTimeFormatter.ISO_DATE

  implicit lazy val javaLocalDateQueryStringBindable: QueryStringBindable[LocalDate] =
    new QueryStringBindable.Parsing[LocalDate](
      LocalDate.parse(_, formatter),
      formatter.format(_),
      (key, _) => s"$key: invalid date"
    )

  implicit lazy val failureTypeQueryStringBindable: QueryStringBindable[Either[NoFailureType, SubmissionItem.FailureType]] =
    new QueryStringBindable.Parsing[Either[NoFailureType, SubmissionItem.FailureType]](
      {
        case "none" => Left(NoFailureType)
        case "sdes" => Right(SubmissionItem.FailureType.Sdes)
        case "timeout" => Right(SubmissionItem.FailureType.Timeout)
      },
      {
        case Left(NoFailureType) => "none"
        case Right(SubmissionItem.FailureType.Sdes) => "sdes"
        case Right(SubmissionItem.FailureType.Timeout) => "timeout"
      },
      (_, _) => "invalid failure type"
    )
}
