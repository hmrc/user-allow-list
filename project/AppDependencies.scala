import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.14.0"
  private val hmrcMongoVersion = "1.0.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"      % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"             % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "internal-auth-client-play-28"   % "1.4.0",
    "uk.gov.hmrc"             %% "crypto"                         % "7.3.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion            % Test,
    "org.scalatestplus"       %% "mockito-3-4"                % "3.2.10.0"                  % "test, it"
  )
}
