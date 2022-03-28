import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "real-time-income-information"

lazy val scoverageSettings = {
  val sCoverageExcludesPattens = List(
    "<empty>",
    "Reverse.*",
    "view.*",
    "config.*",
    ".*(BuildInfo|Routes).*",
    "com.kenshoo.play.*",
    "controllers.javascript",
    ".*Reverse.*Controller"
  )
  Seq(
    ScoverageKeys.coverageExcludedPackages := sCoverageExcludesPattens.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    publishingSettings,
    PlayKeys.playDefaultPort := 9358,
    scoverageSettings,
    RoutesKeys.routesImport := Nil,
    TwirlKeys.templateImports := Nil,
    scalacOptions ++= Seq("-Xfatal-warnings", "-feature"),
    majorVersion := 2,
    resolvers += Resolver.jcenterRepo
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false
  )
