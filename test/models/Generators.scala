package models

import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaStr, const, listOf, posNum, some}
import play.api.libs.json.{JsBoolean, JsNull, JsString}

object Generators {

  implicit class GenOps[A](gen: Gen[A]) {
    def zip[B](other: Gen[B]): Gen[(A, B)] =
      gen.flatMap(a => other.map(b => (a, b)))

    def *>[B](other: Gen[B]): Gen[(A, B)] = zip(other)

    def <*[B](other: Gen[B]): Gen[(B, A)] = other.zip(gen)
  }

  val jsValueGen = Gen.oneOf(alphaStr.map(JsString), Gen.oneOf(true, false).map(JsBoolean), const(JsNull))

  val desSuccessResponseGen =
    (posNum[Int] *> some(listOf(jsValueGen))).map(DesSuccessResponse.tupled)

  val desFilteredSuccessResponseGen =
    (posNum[Int] *> listOf(jsValueGen)).map(DesFilteredSuccessResponse.tupled)

  val desSingleFailureResponseGen =
    (alphaStr *> alphaStr).map(DesSingleFailureResponse.tupled)

  val desMultipleFailureResponseGen =
    listOf(desSingleFailureResponseGen).map(DesMultipleFailureResponse)

  val desUnexpectedResponseGen =
    (alphaStr *> alphaStr).map(DesUnexpectedResponse.tupled)

  val desNoResponseGen =
    (alphaStr *> alphaStr).map(DesNoResponse.tupled)

  val desResponseGen: Gen[DesResponse] =
    Gen.oneOf(
      desSuccessResponseGen,
      desFilteredSuccessResponseGen,
      desSingleFailureResponseGen,
      desMultipleFailureResponseGen,
      desUnexpectedResponseGen,
      desNoResponseGen
    )
}