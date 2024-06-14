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

package repositories

import config.AppConfig
import models.{AllowListEntry, Done, Summary}
import org.mongodb.scala.MongoBulkWriteException
import org.mongodb.scala.model._
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import uk.gov.hmrc.crypto.{OnewayCryptoFactory, PlainText}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import scala.jdk.CollectionConverters._

@Singleton
class AllowListRepository @Inject()(
                                     mongoComponent: MongoComponent,
                                     appConfig: AppConfig,
                                     clock: Clock
                                   )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[AllowListEntry](
    collectionName = "allow-list",
    mongoComponent = mongoComponent,
    domainFormat = AllowListEntry.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("created"),
        IndexOptions()
          .name("createdIdx")
          .expireAfter(appConfig.allowListTtlInDays, TimeUnit.DAYS)
      ),
      IndexModel(
        Indexes.ascending("service", "feature", "hashedValue"),
        IndexOptions()
          .name("serviceFeatureHashedValueIdx")
          .unique(true)
      )
    ),
    extraCodecs = Seq(Codecs.playFormatCodec(Summary.mongoFormat))
  ) {

  private def hashValue(value: String): String =
    OnewayCryptoFactory
      .sha(appConfig.hashKey)
      .hash(PlainText(value))
      .value

  private val duplicateErrorCode = 11000

  def set(service: String, feature: String, values: Set[String]): Future[Done] = {

    val entries = values.map { value =>
      AllowListEntry(service, feature, hashValue(value), clock.instant())
    }

    collection
      .insertMany(
        documents = entries.toSeq,
        options   = InsertManyOptions().ordered(false))
      .toFuture()
      .recover {
        case e: MongoBulkWriteException =>
          if (e.getWriteErrors.asScala.forall(_.getCode == duplicateErrorCode)) {
            Done
          } else {
            throw e
          }
      }
      .map(_ => Done)
  }
  
  def remove(service: String, feature: String, values: Set[String]): Future[Done] = {

    val hashedValues = values.map(hashValue).toSeq

    collection
      .deleteMany(Filters.and(
        Filters.equal("service", service),
        Filters.equal("feature", feature),
        Filters.in("hashedValue", hashedValues: _*)
      )).toFuture
        .map(_ => Done)
  }

  def clear(service: String, feature: String): Future[Done] =
    collection
      .deleteMany(Filters.and(
        Filters.equal("service", service),
        Filters.equal("feature", feature)
      )).toFuture
        .map(_ => Done)

  def check(service: String, feature: String, value: String): Future[Boolean] =
    collection.find(Filters.and(
      Filters.equal("service", service),
      Filters.equal("feature", feature),
      Filters.equal("hashedValue", hashValue(value))
    )).toFuture
      .map(_.nonEmpty)

  def count(service: String, feature: String): Future[Long] =
    collection.countDocuments(Filters.and(
      Filters.equal("service", service),
      Filters.equal("feature", feature)
    )).toFuture

  def summary(service: String): Future[Seq[Summary]] =
    collection.aggregate[Summary](List(
      Aggregates.`match`(Filters.eq("service", service)),
      Aggregates.group("$feature", Accumulators.sum("count", 1))
    )).toFuture
}
