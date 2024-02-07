import sbt._

object AppDependencies {

  val bootstrapVersion = "8.4.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"                     % "1.7.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"             % "8.4.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30"           % "1.10.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.mockito"             %% "mockito-scala-scalatest" % "1.17.14",
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.17.0"
  ).map(_ % Test)

  val integration: Seq[ModuleID] = Seq.empty

  def apply(): Seq[ModuleID] = compile ++ test
}
