package formulation

import org.scalatest.{Matchers, WordSpec}

class AttemptSpec extends WordSpec with Matchers {

  "Attempt" should {
    "convert from Try - Success" in {
      Attempt.fromTry(scala.util.Success(1)) shouldBe Attempt.success(1)
    }
    "convert from Try - Failure" in {
      Attempt.fromTry(scala.util.Failure(new Throwable("error"))) shouldBe Attempt.error("error")
    }
    "convert from Either - Right" in {
      Attempt.fromEither(Right(1)) shouldBe Attempt.success(1)
    }
    "convert from Either - Left" in {
      Attempt.fromEither(Left(new Throwable("error"))) shouldBe Attempt.error("error")
    }
    "convert from Option - Some" in {
      Attempt.fromOption("error")(Some(1)) shouldBe Attempt.success(1)
    }
    "convert from Option - None" in {
      Attempt.fromOption("error")(None) shouldBe Attempt.error("error")
    }
    "convert from Attempt.Success to Either.Right" in {
      Attempt.success(1).toEither shouldBe Right(1)
    }
    "convert from Attempt.Success to Option.Some" in {
      Attempt.success(1).toOption shouldBe Some(1)
    }
    "convert from Attempt.Error to Option.Some" in {
      Attempt.error("error").toOption shouldBe None
    }
  }

}
