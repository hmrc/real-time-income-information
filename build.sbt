import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, itSettings, scalaSettings}

val appName = "real-time-income-information"

ThisBuild / scalaVersion := "3.6.2"
ThisBuild / majorVersion := 2
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Werror",
  "-Wconf:msg=.*-Wunused:s",
  "-Wconf:msg=Flag.*repeatedly:s"
)

val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9358,
    defaultSettings(),
    scalaSettings,
    CodeCoverageSettings.settings,
    retrieveManaged := true,
    libraryDependencies ++= AppDependencies.all,
    RoutesKeys.routesImport := Nil,
    TwirlKeys.templateImports := Nil,
  )

val it: Project = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
