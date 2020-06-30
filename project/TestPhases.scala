import sbt.{ForkOptions, TestDefinition}
import sbt.Tests.{Group, SubProcess}
//TODO what is this?
object TestPhases {
  def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
    tests map {
      test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
