package formulation

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.UUID

import scala.util.Try
import scala.util.control.NonFatal

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

  implicit class RichAvro[A](val fa: Avro[A]) {
    def imap[B](f: A => B)(g: B => A): Avro[B] = self.imap(fa)(f)(g)
    def pmap[B](f: A => Attempt[B])(g: B => A): Avro[B] = self.pmap(fa)(f)(g)
  }
}