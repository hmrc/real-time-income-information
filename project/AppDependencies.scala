import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "5.1.0",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-25",
    "uk.gov.hmrc" %% "auth-client" % "2.32.2-play-25",
    "uk.gov.hmrc" %% "play-auditing" % "4.2.0-play-25",
    "com.eclipsesource" %% "play-json-schema-validator" % "0.9.4"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25",
    "org.scalatest" %% "scalatest" % "3.0.8",
    "org.pegdown" % "pegdown" % "1.6.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
    "org.mockito" % "mockito-core" % "3.2.4",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "com.github.tomakehurst" % "wiremock" % "2.25.1"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test

}
