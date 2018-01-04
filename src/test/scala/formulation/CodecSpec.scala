package formulation

import java.time.{LocalDate, LocalDateTime}
import java.util
import java.util.UUID

import cats.Eq
import cats.implicits._
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class CodecSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks with ArbitraryHelpers {

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
      forAll { (a: BigDecimal) => assert(a, bigDecimal(300, 300)) }
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
      forAll(mapGen) { (a: Map[String, Int]) =>
        assert(a, map(int)(Attempt.success)(identity))
      }
    }

    "work with Either" in {
      forAll { (a: Either[Int, String]) =>
        assert(a, or(int, string))
      }
    }

    "work with ADT" in {
      forAll { (a: BookingProcess) =>
        assert(a, BookingProcess.codec)
      }
    }
  }

  def assert[A](entity: A, avroPart: Avro[A])(implicit E: Eq[A]): Boolean = {

    implicit def codec: Avro[Generic[A]] = Generic.codec(avroPart)

    val record = Generic(entity)

    val eqGeneric = Eq.instance[Attempt[Generic[A]]] {
      case (Attempt.Success(left), Attempt.Success(right)) => E.eqv(left.value, right.value)
      case _ => false
    }

    eqGeneric.eqv(decode[Generic[A]](encode(record)), Attempt.success(record))
  }

  implicit val eqUserId: Eq[UserId] = Eq.fromUniversalEquals[UserId]
  implicit val eqBookingProcess: Eq[BookingProcess] = Eq.fromUniversalEquals[BookingProcess]
  implicit val eqLocalDate: Eq[LocalDate] = Eq.fromUniversalEquals[LocalDate]
  implicit val eqLocalDateTime: Eq[LocalDateTime] = Eq.fromUniversalEquals[LocalDateTime]
  implicit val eqArrayByte: Eq[Array[Byte]] = Eq.instance[Array[Byte]]((x, y) => util.Arrays.equals(x, y))
  implicit def eqSeq[A](implicit E: Eq[A]): Eq[Seq[A]] = Eq.instance[Seq[A]] { case (x, y) => (x zip y).forall(l => E.eqv(l._1, l._2)) }

}
