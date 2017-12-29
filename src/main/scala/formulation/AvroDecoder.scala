package formulation

import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8

import cats.implicits._
import cats._

trait AvroDecoder[A] {
  def decode(data: AvroData): Either[String, A]
}

object AvroDecoder {

  import scala.collection.JavaConverters._

  def partial[A](f: PartialFunction[AvroData, Either[String, A]]) = new AvroDecoder[A] {
    override def decode(data: AvroData): Either[String, A] = f.applyOrElse(data, (x: AvroData) => Left(s"Unexpected $x"))
  }

  def toAvroData(anyRef: Any): AvroData = anyRef match {
    case x: Int => AvroData.Integer(x)
    case x: String => AvroData.Str(x)
    case x: Utf8 => AvroData.Str(x.toString)
    case x: GenericRecord => AvroData.Record(x)
    case x: java.util.Collection[_] => AvroData.Items(x.asScala.toList.map(x => toAvroData(x)))
    case null => AvroData.Null
    case x => sys.error(s"Unrecognized data type: ${x.getClass}")
  }

  implicit val interpreter: AvroAlgebra[AvroDecoder] = new AvroAlgebra[AvroDecoder] with AvroDecoderRecordN {

    override val int: AvroDecoder[Int] = partial { case AvroData.Integer(v) => Right(v) }
    override val string: AvroDecoder[String] = partial { case AvroData.Str(v) => Right(v) }

    override def imap[A, B](fa: AvroDecoder[A])(f: A => B)(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(data: AvroData): Either[String, B] = fa.decode(data).map(f)
    }

    override def option[A](from: AvroDecoder[A]): AvroDecoder[Option[A]] = new AvroDecoder[Option[A]] {
      override def decode(data: AvroData): Either[String, Option[A]] = data match {
        case AvroData.Null => Right(None)
        case x => from.decode(x).map(Some.apply)
      }
    }

    override def list[A](of: AvroDecoder[A]): AvroDecoder[List[A]] =
      partial { case AvroData.Items(items) => items.traverse[Either[String, ?], A](of.decode) }

    override def pmap[A, B](fa: AvroDecoder[A])(f: A => Either[String, B])(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(data: AvroData): Either[String, B] = fa.decode(data).flatMap(f)
    }
  }

  implicit def apply[A](implicit A: Avro[A]): AvroDecoder[A] = A.apply[AvroDecoder]
}