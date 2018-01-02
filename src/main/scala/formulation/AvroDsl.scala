package formulation

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.UUID

import scala.util.Try

trait Avro[A] {
  def apply[F[_] : AvroAlgebra]: F[A]
}

trait AvroDsl extends AvroDslRecordN { self =>

  val int: Avro[Int] = new Avro[Int] {
    override def apply[F[_] : AvroAlgebra]: F[Int] = implicitly[AvroAlgebra[F]].int
  }

  val string: Avro[String] = new Avro[String] {
    override def apply[F[_] : AvroAlgebra]: F[String] = implicitly[AvroAlgebra[F]].string
  }

  val bool: Avro[Boolean] = new Avro[Boolean] {
    override def apply[F[_] : AvroAlgebra]: F[Boolean] = implicitly[AvroAlgebra[F]].bool
  }

  val long: Avro[Long] = new Avro[Long] {
    override def apply[F[_] : AvroAlgebra]: F[Long] = implicitly[AvroAlgebra[F]].long
  }

  val double: Avro[Double] = new Avro[Double] {
    override def apply[F[_] : AvroAlgebra]: F[Double] = implicitly[AvroAlgebra[F]].double
  }

  val float: Avro[Float] = new Avro[Float] {
    override def apply[F[_] : AvroAlgebra]: F[Float] = implicitly[AvroAlgebra[F]].float
  }

  val byteArray: Avro[Array[Byte]] = new Avro[Array[Byte]] {
    override def apply[F[_] : AvroAlgebra]: F[Array[Byte]] = implicitly[AvroAlgebra[F]].byteArray
  }

  def bigDecimal(scale: Int = 2, precision: Int = 8): Avro[BigDecimal] = new Avro[BigDecimal] {
    override def apply[F[_] : AvroAlgebra]: F[BigDecimal] = implicitly[AvroAlgebra[F]].bigDecimal(scale, precision)
  }

  val uuid: Avro[UUID] =
    string.pmap(str => Attempt.fromTry(Try(UUID.fromString(str))))(_.toString)

  val localDate: Avro[LocalDate] =
    string.pmap(str => Attempt.fromTry(Try(LocalDate.parse(str))))(_.format(DateTimeFormatter.ISO_LOCAL_DATE))

  val localDateTime: Avro[LocalDateTime] =
    string.pmap(str => Attempt.fromTry(Try(LocalDateTime.parse(str))))(_.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

  def imap[A, B](fa: Avro[A])(f: A => B)(g: B => A): Avro[B] = new Avro[B] {
    override def apply[F[_] : AvroAlgebra]: F[B] = implicitly[AvroAlgebra[F]].imap(fa.apply[F])(f)(g)
  }

  def pmap[A, B](fa: Avro[A])(f: A => Attempt[B])(g: B => A): Avro[B] = new Avro[B] {
    override def apply[F[_] : AvroAlgebra]: F[B] = implicitly[AvroAlgebra[F]].pmap(fa.apply[F])(f)(g)
  }

  def option[A](value: Avro[A]): Avro[Option[A]] = new Avro[Option[A]] {
    override def apply[F[_] : AvroAlgebra]: F[Option[A]] = implicitly[AvroAlgebra[F]].option(value.apply[F])
  }

  def list[A](of: Avro[A]): Avro[List[A]] = new Avro[List[A]] {
    override def apply[F[_] : AvroAlgebra]: F[List[A]] = implicitly[AvroAlgebra[F]].list(of.apply[F])
  }

  def set[A](of: Avro[A]): Avro[Set[A]] = new Avro[Set[A]] {
    override def apply[F[_] : AvroAlgebra]: F[Set[A]] = implicitly[AvroAlgebra[F]].set(of.apply[F])
  }

  def vector[A](of: Avro[A]): Avro[Vector[A]] = new Avro[Vector[A]] {
    override def apply[F[_] : AvroAlgebra]: F[Vector[A]] = implicitly[AvroAlgebra[F]].vector(of.apply[F])
  }

  def seq[A](of: Avro[A]): Avro[Seq[A]] = new Avro[Seq[A]] {
    override def apply[F[_] : AvroAlgebra]: F[Seq[A]] = implicitly[AvroAlgebra[F]].seq(of.apply[F])
  }

  def map[K, V](of: Avro[V], contramapKey: K => String, mapKey: String => Attempt[K]): Avro[Map[K, V]] = new Avro[Map[K, V]] {
    override def apply[F[_] : AvroAlgebra]: F[Map[K, V]] = implicitly[AvroAlgebra[F]].map(of.apply[F], contramapKey, mapKey)
  }

  implicit class RichAvro[A](val fa: Avro[A]) {
    def imap[B](f: A => B)(g: B => A): Avro[B] = self.imap(fa)(f)(g)
    def pmap[B](f: A => Attempt[B])(g: B => A): Avro[B] = self.pmap(fa)(f)(g)
  }
}