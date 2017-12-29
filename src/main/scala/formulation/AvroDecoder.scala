package formulation

import cats.implicits._
import org.apache.avro.util.Utf8

import scala.annotation.implicitNotFound

@implicitNotFound(msg = "AvroDecoder[${A}] not found, did you implicitly define Avro[${A}]?")
trait AvroDecoder[A] {
  def decode(data: Any): Either[Throwable, A]
}

object AvroDecoder {

  import scala.collection.JavaConverters._

  def partial[A](f: PartialFunction[Any, Either[Throwable, A]]): AvroDecoder[A] = new AvroDecoder[A] {
    override def decode(data: Any): Either[Throwable, A] = f.applyOrElse(data, (x: Any) => Left(new Throwable(s"Unexpected $x")))
  }

  implicit val interpreter: AvroAlgebra[AvroDecoder] = new AvroAlgebra[AvroDecoder] with AvroDecoderRecordN {

    override val int: AvroDecoder[Int] = partial { case v: Int => Right(v) }
    override val string: AvroDecoder[String] = partial { case v: Utf8 => Right(v.toString) }

    override def imap[A, B](fa: AvroDecoder[A])(f: A => B)(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(data: Any): Either[Throwable, B] = fa.decode(data).map(f)
    }

    override def option[A](from: AvroDecoder[A]): AvroDecoder[Option[A]] = new AvroDecoder[Option[A]] {
      override def decode(data: Any): Either[Throwable, Option[A]] = data match {
        case null => Right(None)
        case x => from.decode(x).map(Some.apply)
      }
    }

    override def list[A](of: AvroDecoder[A]): AvroDecoder[List[A]] =
      partial { case x: java.util.Collection[_] => x.asScala.toList.traverse[Either[Throwable, ?], A](of.decode) }

    override def pmap[A, B](fa: AvroDecoder[A])(f: A => Either[Throwable, B])(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(data: Any): Either[Throwable, B] = fa.decode(data).flatMap(f)
    }
  }

  implicit def apply[A](implicit A: Avro[A]): AvroDecoder[A] = A.apply[AvroDecoder]
}