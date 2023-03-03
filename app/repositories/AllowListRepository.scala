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
import models.{AllowListEntry, Done}
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes, InsertManyOptions}
import uk.gov.hmrc.crypto.{OnewayCryptoFactory, PlainText}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
    )
  ) {

  private def hashValue(value: String): String = {
    val saltedValue = s"${appConfig.salt}$value"
    OnewayCryptoFactory.sha(appConfig.hashKey).hash(PlainText(saltedValue)).value
  }

  def set(service: String, feature: String, values: Set[String]): Future[Done] = {

    val entries = values.map {
      value =>
        AllowListEntry(service, feature, hashValue(value), clock.instant())
    }

    collection
      .insertMany(
        documents = entries.toSeq,
        options   = InsertManyOptions().ordered(false))
      .toFuture()
      .recover {
        case _ => Done
      }
      .map(_ => Done)
  }

  def remove(service: String, feature: String, value: Set[String]): Future[Done] = ???

  def clear(service: String, feature: String): Future[Done] = ???

  def check(service: String, feature: String, value: String): Future[Boolean] = ???

  def count(service: String, feature: String): Future[Int] = ???

  def summary(service: String): Future[Map[String, Int]] = ???
}
