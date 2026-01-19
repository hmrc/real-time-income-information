import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "view.*",
    "config.*",
    ".*(BuildInfo|Routes).*",
    "controllers.javascript",
    ".*Reverse.*Controller"
  )
  private val excludedFiles: Seq[String] = Seq(
    ".*DesResponse.scala.*",
  )

  // case classes with no added functionality so no requirement to test
  // other than default Reads, Writes or Format
  private val implicitOFormatObjects: Seq[String] = Seq(
    ".*DesMatchingRequest.*",
    ".*DesSingleFailureResponse.*",
    ".*DesMultipleFailureResponse.*",
    ".*RequestDetails.*"
  )

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := (excludedPackages ++ implicitOFormatObjects).mkString(";"),
    ScoverageKeys.coverageExcludedFiles :=(excludedFiles.mkString(";")),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
