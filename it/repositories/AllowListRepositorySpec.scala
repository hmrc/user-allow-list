package repositories

import config.AppConfig
import models.AllowListEntry
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}

import scala.concurrent.ExecutionContext.Implicits.global

class AllowListRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[AllowListEntry]
    with OptionValues
    with ScalaFutures {

  private val config = Configuration(
    "appName" -> "user-allow-list",
    "mongodb.allowListTtlInDays" -> 1
  )
  private val appConfig = new AppConfig(config)
  private val clock = Clock.fixed(Instant.now, ZoneId.systemDefault())

  protected override val repository = new AllowListRepository(mongoComponent = mongoComponent, appConfig = appConfig, clock = clock)
}
