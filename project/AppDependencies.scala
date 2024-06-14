import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.0.0"
  private val hmrcMongoVersion = "2.0.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"      % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"             % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "internal-auth-client-play-30"   % "3.0.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion            % Test,
    "org.scalatestplus"       %% "mockito-3-4"                % "3.2.10.0"                  % Test
  )

  val integration = Seq.empty[ModuleID]
}
