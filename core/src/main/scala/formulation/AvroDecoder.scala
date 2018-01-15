package formulation

import java.nio.ByteBuffer
import java.time.Instant
import java.util.UUID

import cats.Semigroupal
import org.apache.avro.{Conversions, LogicalTypes, Schema}
import org.apache.avro.util.Utf8
import shapeless.CNil

import scala.annotation.implicitNotFound
import scala.util.Try
import cats.implicits._

@implicitNotFound(msg = "AvroDecoder[${A}] not found, did you implicitly define Avro[${A}]?")
sealed trait AvroDecoder[A] { self =>
  def decode(schema: Schema, data: Any): Either[Throwable, A]

  def map[B](f: A => B): AvroDecoder[B] = new AvroDecoder[B] {
    override def decode(schema: Schema, data: Any): Either[Throwable, B] = self.decode(schema, data).map(f)
  }

  def andThen[B](f: A => Either[Throwable, B]): AvroDecoder[B] = new AvroDecoder[B] {
    override def decode(schema: Schema, data: Any): Either[Throwable, B] = self.decode(schema, data).flatMap(f)
  }
}

object AvroDecoder {

  import scala.collection.JavaConverters._

  def partial[A](f: PartialFunction[Any, Either[Throwable, A]]): AvroDecoder[A] = new AvroDecoder[A] {
    override def decode(schema: Schema, data: Any): Either[Throwable, A] = f.applyOrElse(data, (x: Any) => Left(new Throwable(s"Unexpected '$x' (class: ${x.getClass})")))
  }

  def partialWithSchema[A](f: PartialFunction[(Schema, Any), Either[Throwable, A]]): AvroDecoder[A] = new AvroDecoder[A] {
    override def decode(schema: Schema, data: Any): Either[Throwable, A] =
      f.applyOrElse(schema -> data, (x: (Schema, Any)) => Left(new Throwable(s"Unexpected '$x' (class: ${x.getClass})")))
  }

  def fail[A](error: String): AvroDecoder[A] = new AvroDecoder[A] {
    override def decode(schema: Schema, data: Any): Either[Throwable, A] = Left(new Throwable(error))
  }

  implicit val interpreter: AvroAlgebra[AvroDecoder] = new AvroAlgebra[AvroDecoder] with AvroDecoderRecordN {

    override val int: AvroDecoder[Int] = partial { case v: Int => Right(v) }
    override val string: AvroDecoder[String] = partial { case v: Utf8 => Right(v.toString) }
    override val bool: AvroDecoder[Boolean] = partial { case v: Boolean => Right(v) }
    override val float: AvroDecoder[Float] = partial { case v: Float => Right(v) }
    override val byteArray: AvroDecoder[Array[Byte]] = partial[Array[Byte]] { case v: ByteBuffer => Right(v.array()) }
    override val double: AvroDecoder[Double] = partial { case v: Double => Right(v) }
    override val long: AvroDecoder[Long] = partial { case v: Long => Right(v) }
    override val cnil: AvroDecoder[CNil] = fail("Unable to decode cnil")

    override val uuid: AvroDecoder[UUID] = string.andThen(str => Either.fromTry(Try(UUID.fromString(str))))

    override val instant: AvroDecoder[Instant] = long.andThen(ts => Either.fromTry(Try(Instant.ofEpochMilli(ts))))

    override def bigDecimal(scale: Int, precision: Int): AvroDecoder[BigDecimal] = partial[BigDecimal] { case v: ByteBuffer =>

      val decimalType = LogicalTypes.decimal(precision, scale)
      val decimalConversion = new Conversions.DecimalConversion

      Either.fromTry(Try(decimalConversion.fromBytes(v, null, decimalType)))
    }

    override def imap[A, B](fa: AvroDecoder[A])(f: A => B)(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(schema: Schema, data: Any): Either[Throwable, B] = fa.decode(schema, data).map(f)
    }

    override def option[A](from: AvroDecoder[A]): AvroDecoder[Option[A]] = new AvroDecoder[Option[A]] {
      override def decode(schema: Schema, data: Any): Either[Throwable, Option[A]] = data match {
        case null => Right(None)
        case x => from.decode(schema, x).map(Some.apply)
      }
    }

    override def list[A](of: AvroDecoder[A]): AvroDecoder[List[A]] =
      partialWithSchema {
        case (s, x: Array[_]) =>
          x.toList.traverse[Either[Throwable, ?], A](y => of.decode(s, y))
        case (s, x: java.util.Collection[_]) =>
          x.asScala.toList.traverse[Either[Throwable, ?], A](y => of.decode(s, y))
      }

    override def set[A](of: AvroDecoder[A]): AvroDecoder[Set[A]] =
      list(of).map(_.toSet)

    override def vector[A](of: AvroDecoder[A]): AvroDecoder[Vector[A]] =
      list(of).map(_.toVector)

    override def seq[A](of: AvroDecoder[A]): AvroDecoder[Seq[A]] =
      list(of).map(_.toSeq)

    override def map[K, V](of: AvroDecoder[V])(mapKey: String => Either[Throwable, K])(contramapKey: K => String): AvroDecoder[Map[K, V]] =
      partialWithSchema { case (schema, x: java.util.Map[_, _]) =>
        x.asScala
          .toMap
          .map { case (k, v) => k.toString -> v }
          .foldRight(Right(Map.empty): Either[Throwable, Map[K, V]]) { case ((key, value), init) =>
            Semigroupal.map3[Either[Throwable, ?], K, V, Map[K, V], Map[K, V]](mapKey(key), of.decode(schema.getValueType, value), init) { case (k, v, acc) => acc + (k -> v) }
          }
      }

    override def pmap[A, B](fa: AvroDecoder[A])(f: A => Either[Throwable, B])(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(schema: Schema, data: Any): Either[Throwable, B] = fa.decode(schema, data).flatMap(f)
    }

    override def or[A, B](fa: AvroDecoder[A], fb: AvroDecoder[B]): AvroDecoder[Either[A, B]] = {
      partialWithSchema { case (s, el) =>
        def loop(schemas: List[Schema]): Either[Throwable, Either[A, B]] = schemas match {
          case x :: xs => fa.decode(x, el).map(Left.apply) orElse fb.decode(x, el).map(Right.apply) orElse loop(xs)
          case Nil => Left(new Throwable("Unable to match anything"))
        }

        s.getType match {
          case Schema.Type.UNION => loop(s.getTypes.asScala.toList)
          case _ => fa.decode(s, el).map(Left.apply) orElse fb.decode(s, el).map(Right.apply)
        }
      }
    }
  }

  implicit def apply[A](implicit A: Avro[A]): AvroDecoder[A] = A.apply[AvroDecoder]


}