import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings, itSettings}

val appName = "real-time-income-information"

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 2
ThisBuild / scalacOptions ++= Seq("-Xfatal-warnings", "-feature")

val scoverageSettings: Seq[Def.Setting[?]] = {
  val sCoverageExcludesPattens: List[String] = List(
    "<empty>",
    "Reverse.*",
    "view.*",
    "config.*",
    ".*(BuildInfo|Routes).*",
    "controllers.javascript",
    ".*Reverse.*Controller"
  )
  Seq(
    ScoverageKeys.coverageExcludedPackages := sCoverageExcludesPattens.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9358,
    defaultSettings(),
    scalaSettings,
    scoverageSettings,
    retrieveManaged := true,
    libraryDependencies ++= AppDependencies.all,
    RoutesKeys.routesImport := Nil,
    TwirlKeys.templateImports := Nil,
  )

val it: Project = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
