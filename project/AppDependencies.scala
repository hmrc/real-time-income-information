import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val hmrcBoostrapVersion       = "8.6.0"
  private val hmrcMongoVersion          = "1.9.0"
  private val hmrcDomainVersion         = "9.0.0"

  private val playVersion               = "play-30"


  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion"  % hmrcBoostrapVersion,
    "uk.gov.hmrc"       %% s"domain-$playVersion"             % hmrcDomainVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"         % hmrcMongoVersion,
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % hmrcBoostrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.mockito"             % "mockito-core"                  % "5.12.0",
    "org.scalatestplus"      %% "scalacheck-1-17"               % "3.2.18.0",
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test

}
