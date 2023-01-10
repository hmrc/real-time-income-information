import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val hmrcMongoVersion = "0.74.0"


  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "7.12.0",
    "uk.gov.hmrc"       %% "domain"                    % "8.1.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"          % "3.2.12",
    "org.scalatestplus"      %% "scalacheck-1-15"    % "3.2.11.0",
    "org.pegdown"             % "pegdown"            % "1.6.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
    "org.mockito"             % "mockito-core"       % "4.6.1",
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current,
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.29.1",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.62.2",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test

}
