import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "5.14.0",
    "uk.gov.hmrc"       %% "domain"                    % "6.2.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % "0.60.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"          % "3.2.9",
    "org.pegdown"             % "pegdown"            % "1.6.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
    "org.mockito"             % "mockito-core"       % "3.4.0",
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current,
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.26.3",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.35.10",
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-28" % "0.60.0"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test

}
