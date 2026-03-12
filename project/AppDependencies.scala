import sbt._

object AppDependencies {

  val bootstrapVersion = "10.7.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"                     % "2.12.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"             % "12.32.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30"  % "3.5.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30"           % "4.3.0",
    "org.typelevel"     %% "cats-core"                              % "2.8.0",
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.scalatestplus"       %% "mockito-4-11"            % "3.2.18.0",
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.18.0"
  ).map(_ % Test)

  val integration: Seq[ModuleID] = Seq.empty

  def apply(): Seq[ModuleID] = compile ++ test
}
