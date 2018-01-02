package formulation

import java.nio.ByteBuffer

import org.apache.avro.{Conversions, LogicalTypes, Schema}

import scala.annotation.implicitNotFound

@implicitNotFound(msg = "AvroEncoder[${A}] not found, did you implicitly define Avro[${A}]?")
trait AvroEncoder[A] {
  def encode(schema: Schema, value: A): Any
}

object AvroEncoder {

  import scala.collection.JavaConverters._

  def by[A, B](fa: AvroEncoder[A])(f: B => A): AvroEncoder[B] = new AvroEncoder[B] {
    override def encode(schema: Schema, value: B): Any = fa.encode(schema, f(value))
  }

  def create[A](f: (Schema, A) => Any): AvroEncoder[A] = new AvroEncoder[A] {
    override def encode(schema: Schema, value: A): Any = f(schema, value)
  }

  implicit def apply[A](implicit A: Avro[A]): AvroEncoder[A] = A.apply[AvroEncoder]

  implicit val interpreter: AvroAlgebra[AvroEncoder] = new AvroAlgebra[AvroEncoder] with AvroEncoderRecordN {

    override val string: AvroEncoder[String] = AvroEncoder.create((_, v) => v)

    override val int: AvroEncoder[Int] = AvroEncoder.create((_, v) => v)

    override val bool: AvroEncoder[Boolean] = AvroEncoder.create((_, v) => v)

    override val float: AvroEncoder[Float] = AvroEncoder.create((_, v) => v)

    override val byteArray: AvroEncoder[Array[Byte]] = AvroEncoder.create((_, v) => ByteBuffer.wrap(v) )

    override val double: AvroEncoder[Double] = AvroEncoder.create((_, v) => v)

    override val long: AvroEncoder[Long] = AvroEncoder.create((_, v) => v)

    override def bigDecimal(scale: Int, precision: Int): AvroEncoder[BigDecimal] = AvroEncoder.create { case (_, v: BigDecimal) =>
      val decimalType = LogicalTypes.decimal(precision, scale)
      val decimalConversion = new Conversions.DecimalConversion

      decimalConversion.toBytes(v.setScale(scale).bigDecimal, null, decimalType)
    }

    override def imap[A, B](fa: AvroEncoder[A])(f: A => B)(g: B => A): AvroEncoder[B] = AvroEncoder.create((_, v) => g(v))

    override def option[A](from: AvroEncoder[A]): AvroEncoder[Option[A]] = AvroEncoder.create {
      case (schema, Some(value)) => from.encode(schema, value)
      case (_, None) => null
    }

    override def list[A](of: AvroEncoder[A]): AvroEncoder[List[A]] =
      AvroEncoder.create((schema, list) => list.map(of.encode(schema.getElementType, _)).asJava)

    override def pmap[A, B](fa: AvroEncoder[A])(f: A => Attempt[B])(g: B => A): AvroEncoder[B] =
      AvroEncoder.create((schema, b) => fa.encode(schema, g(b)))

    override def set[A](of: AvroEncoder[A]): AvroEncoder[Set[A]] = by(list(of))(_.toList)

    override def vector[A](of: AvroEncoder[A]): AvroEncoder[Vector[A]] = by(list(of))(_.toList)

    override def seq[A](of: AvroEncoder[A]): AvroEncoder[Seq[A]] = by(list(of))(_.toList)

    override def map[V](of: AvroEncoder[V]): AvroEncoder[Map[String, V]] =
      AvroEncoder.create((schema, v) => v.mapValues(of.encode(schema.getValueType, _)).asJava)
  }
}