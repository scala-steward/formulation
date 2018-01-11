package formulation

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryHelpers {

  val mapGen: Gen[Map[String, Int]] = {
    def genKv = for {
      key <- Gen.alphaLowerStr
      value <- Gen.choose(Int.MinValue, Int.MaxValue)
    } yield key -> value

    Gen.nonEmptyListOf(genKv).map(_.toMap)
  }

  implicit val bookingProcessArb: Arbitrary[BookingProcess] = Arbitrary {
    def genNotStarted = Gen.const(BookingProcess.NotStarted(0))
    def genCancelled = Gen.const(BookingProcess.Cancelled(2))
    def genDateSelected = localDateTineArb.arbitrary.map(date => BookingProcess.DateSelected(1, date))

    for {
      caze <- Gen.choose(0, 2)
      process <- caze match {
        case 0 => genNotStarted
        case 1 => genDateSelected
        case 2 => genCancelled
      }
    } yield process
  }

  implicit val uuidArb: Arbitrary[UUID] = Arbitrary(Gen.uuid)
  implicit val localDateArb: Arbitrary[LocalDate] = Arbitrary {
    for {
      year <- Gen.choose(1970, 2050)
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, 28)
    } yield LocalDate.of(year, month, day)
  }

  implicit val instantArb: Arbitrary[Instant] = Arbitrary {
    Gen.choose(0, Long.MaxValue).map(ts => Instant.ofEpochMilli(ts))
  }

  implicit val localDateTineArb: Arbitrary[LocalDateTime] = Arbitrary {
    for {
      year <- Gen.choose(1970, 2050)
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, 28)
      hour <- Gen.choose(0, 23)
      minute <- Gen.choose(0, 59)
      second <- Gen.choose(0, 59)
    } yield LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second))
  }

  implicit val userV1Arb: Arbitrary[UserV1] = Arbitrary {
    for {
      userId <- Gen.choose(0, Int.MaxValue).map(UserId)
      username <- Gen.alphaStr
      email <- Gen.alphaStr
      password <- Gen.alphaStr
    } yield UserV1(userId, username, email, password)
  }

  implicit val userV2Arb: Arbitrary[UserV2] = Arbitrary {
    for {
      userId <- Gen.choose(0, Int.MaxValue).map(UserId)
      username <- Gen.alphaStr
      email <- Gen.alphaStr
      password <- Gen.alphaStr
      age <- Gen.option(Gen.choose(0, 99))
      countries <- Gen.listOf(Gen.alphaStr)
      bookingProcess <- bookingProcessArb.arbitrary
      money <- Gen.choose(0, Int.MaxValue).map(BigDecimal.apply)
    } yield UserV2(userId, username, email, password, age, countries, bookingProcess, money)
  }
}
