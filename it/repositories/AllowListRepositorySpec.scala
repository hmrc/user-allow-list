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
import models.AllowListEntry
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import uk.gov.hmrc.crypto.{OnewayCryptoFactory, PlainText}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class AllowListRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[AllowListEntry]
    with OptionValues
    with ScalaFutures {

  private val hashKey = "VbLZDxE++BTKOEP98xxiYcJAlBer8d23nZ//9OI6IeaaroCJWgPo2MQrx6T1KI99WZyTTmabXHGo87Wgb2dS/g=="
  private val config = Configuration(
    "appName" -> "user-allow-list",
    "mongodb.allowListTtlInDays" -> 1,
    "mongodb.allowListSalt" -> "salt",
    "mongodb.hashKey" -> hashKey
  )
  private val appConfig = new AppConfig(config)
  private val fixedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val clock = Clock.fixed(fixedInstant, ZoneId.systemDefault())

  protected override val repository = new AllowListRepository(mongoComponent = mongoComponent, appConfig = appConfig, clock = clock)

  ".set" - {

    "must save an entry that does not already exist in the repository, hashing the value" in {

      val service = "service"
      val feature = "feature"
      val value = "value"

      repository.set(service, feature, Set(value)).futureValue
      val insertedRecord = findAll().futureValue.head

      val expectedHashedValue = OnewayCryptoFactory.sha(hashKey).hash(PlainText("saltvalue")).value

      insertedRecord mustEqual AllowListEntry(service, feature, expectedHashedValue, fixedInstant)
    }

    "must save multiple different entries, hashing their values" in {

      val service = "service"
      val feature = "feature"
      val value1 = "value1"
      val value2 = "value2"

      repository.set(service, feature, Set(value1, value2)).futureValue
      val insertedRecords = findAll().futureValue

      val expectedHashedValue1 = OnewayCryptoFactory.sha(hashKey).hash(PlainText("saltvalue1")).value
      val expectedHashedValue2 = OnewayCryptoFactory.sha(hashKey).hash(PlainText("saltvalue2")).value

      insertedRecords must contain theSameElementsAs Seq(
        AllowListEntry(service, feature, expectedHashedValue1, fixedInstant),
        AllowListEntry(service, feature, expectedHashedValue2, fixedInstant)
      )
    }

    "must insert new records without failing when given some new records and some duplicates" in {

      val service = "service"
      val feature = "feature"
      val value1 = "value1"
      val value2 = "value2"
      val hashedValue1 = OnewayCryptoFactory.sha(hashKey).hash(PlainText("saltvalue1")).value
      val existingEntryCreated = fixedInstant.minusSeconds(1)
      val existingEntry = AllowListEntry(service, feature, hashedValue1, existingEntryCreated)

      insert(existingEntry).futureValue

      repository.set(service, feature, Set(value1, value2)).futureValue

      val expectedHashedValue2 = OnewayCryptoFactory.sha(hashKey).hash(PlainText("saltvalue2")).value

      findAll().futureValue must contain theSameElementsAs Seq(
        existingEntry,
        AllowListEntry(service, feature, expectedHashedValue2, fixedInstant)
      )
    }
  }
}
