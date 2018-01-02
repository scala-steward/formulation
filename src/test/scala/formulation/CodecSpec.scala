package formulation

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util
import java.util.UUID

import cats.Eq
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

case class Generic[T](value: T)

case class UserId(id: Int)

class CodecSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks {

  "Codec" should {

    "work with Int" in {
      forAll { (a: Int) => assert(a, int) }
    }
    "work with Imap" in {
      forAll { (a: Int) => assert(UserId(a), int.imap(UserId.apply)(_.id)) }
    }
    "work with String" in {
      forAll { (a: String) => assert(a, string) }
    }
    "work with Float" in {
      forAll { (a: Float) => assert(a, float) }
    }
    "work with Bool" in {
      forAll { (a: Boolean) => assert(a, bool) }
    }
    "work with Double" in {
      forAll { (a: Double) => assert(a, double) }
    }
    "work with Long" in {
      forAll { (a: Long) => assert(a, long) }
    }
    "work with UUID" in {
      forAll { (a: UUID) => assert(a, uuid) }
    }
    "work with LocalDate" in {
      forAll { (a: LocalDate) => assert(a, localDate) }
    }
    "work with LocalDateTime" in {
      forAll { (a: LocalDateTime) => assert(a, localDateTime) }
    }
    "work with ByteArray" in {
      forAll { (a: Array[Byte]) => assert(a, byteArray) }
    }
    "work with BigDecimal" in {
      forAll { (a: BigDecimal) => assert(a, bigDecimal(300, 8)) }
    }
    "work with Option" in {
      forAll { (a: Option[Int]) => assert(a, option(int)) }
    }
    "work with List" in {
      forAll { (a: List[Int]) => assert(a, list(int)) }
    }
    "work with Set" in {
      forAll { (a: Set[Int]) => assert(a, set(int)) }
    }
    "work with Seq" in {
      forAll { (a: Seq[Int]) => assert(a, seq(int)) }
    }
    "work with Vector" in {
      forAll { (a: Vector[Int]) => assert(a, vector(int)) }
    }
    "work with Map" in {
      forAll { (a: Map[String, Int]) =>
        whenever(a.keySet.forall(_.nonEmpty)) { assert(a, map(int)) }
      }
    }
  }

  def assert[A](entity: A, avroPart: Avro[A])(implicit E: Eq[A]) = {

    implicit val avro: Avro[Generic[A]] =
      record1("formulation", "Generic")(Generic.apply[A])("value" -> Member(avroPart, _.value))

    val record = Generic(entity)

    val eqGeneric = Eq.instance[Attempt[Generic[A]]] {
      case (Attempt.Success(left), Attempt.Success(right)) => E.eqv(left.value, right.value)
      case _ => false
    }

    eqGeneric.eqv(decode[Generic[A]](encode(record)), Attempt.success(record))
  }

  implicit val uuidArb: Arbitrary[UUID] = Arbitrary(Gen.uuid)
  implicit val localDateArb: Arbitrary[LocalDate] = Arbitrary {
    for {
      year <- Gen.choose(1970, 2050)
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, 28)
    } yield LocalDate.of(year, month, day)
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

  implicit val eqUserId: Eq[UserId] = Eq.fromUniversalEquals[UserId]
  implicit val eqLocalDate: Eq[LocalDate] = Eq.fromUniversalEquals[LocalDate]
  implicit val eqLocalDateTime: Eq[LocalDateTime] = Eq.fromUniversalEquals[LocalDateTime]
  implicit val eqArrayByte: Eq[Array[Byte]] = Eq.instance[Array[Byte]]((x, y) => util.Arrays.equals(x, y))
  implicit def eqSeq[A](implicit E: Eq[A]): Eq[Seq[A]] = Eq.instance[Seq[A]] { case (x, y) => (x zip y).forall(l => E.eqv(l._1, l._2)) }
}
