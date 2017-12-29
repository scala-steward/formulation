package formulation

import cats._
import org.apache.avro.Schema

import scala.annotation.implicitNotFound

@implicitNotFound(msg = "AvroEncoder[${A}] not found, did you implicitly define Avro[${A}]?")
trait AvroEncoder[A] {
  def encode(schema: Schema, value: A): Any
}

object AvroEncoder {

  import scala.collection.JavaConverters._

  def create[A](f: (Schema, A) => Any): AvroEncoder[A] = new AvroEncoder[A] {
    override def encode(schema: Schema, value: A): Any = f(schema, value)
  }

  implicit def apply[A](implicit A: Avro[A]): AvroEncoder[A] = A.apply[AvroEncoder]

  implicit val contravariant: Contravariant[AvroEncoder] = new Contravariant[AvroEncoder] {
    override def contramap[A, B](fa: AvroEncoder[A])(f: B => A): AvroEncoder[B] = create((schema, b) => f(b))
  }

  implicit val interpreter: AvroAlgebra[AvroEncoder] = new AvroAlgebra[AvroEncoder] with AvroEncoderRecordN {

    override val string: AvroEncoder[String] = AvroEncoder.create((_, v) => v)

    override val int: AvroEncoder[Int] = AvroEncoder.create((_, v) => v)

    override def imap[A, B](fa: AvroEncoder[A])(f: A => B)(g: B => A): AvroEncoder[B] = AvroEncoder.create((_, v) => g(v))

    override def option[A](from: AvroEncoder[A]): AvroEncoder[Option[A]] = AvroEncoder.create {
      case (schema, Some(value)) => from.encode(schema, value)
      case (_, None) => null
    }

    override def list[A](of: AvroEncoder[A]): AvroEncoder[List[A]] =
      AvroEncoder.create((schema, list) => list.map(of.encode(schema.getElementType, _)).asJava)

    override def pmap[A, B](fa: AvroEncoder[A])(f: A => Either[Throwable, B])(g: B => A): AvroEncoder[B] =
      AvroEncoder.create((schema, b) => fa.encode(schema, g(b)))
  }
}