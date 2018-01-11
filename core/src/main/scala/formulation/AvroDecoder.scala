package formulation

import java.nio.ByteBuffer

import org.apache.avro.{Conversions, LogicalTypes, Schema}
import org.apache.avro.util.Utf8
import shapeless.CNil

import scala.annotation.implicitNotFound
import scala.util.Try

import cats.implicits._

@implicitNotFound(msg = "AvroDecoder[${A}] not found, did you implicitly define Avro[${A}]?")
sealed trait AvroDecoder[A] { self =>
  def decode(schema: Schema, data: Any): Attempt[A]

  def map[B](f: A => B): AvroDecoder[B] = new AvroDecoder[B] {
    override def decode(schema: Schema, data: Any): Attempt[B] = self.decode(schema, data).map(f)
  }
}

object AvroDecoder {

  import scala.collection.JavaConverters._

  def partial[A](f: PartialFunction[Any, Attempt[A]]): AvroDecoder[A] = new AvroDecoder[A] {
    override def decode(schema: Schema, data: Any): Attempt[A] = f.applyOrElse(data, (x: Any) => Attempt.error(s"Unexpected '$x' (class: ${x.getClass})"))
  }

  def partialWithSchema[A](f: PartialFunction[(Schema, Any), Attempt[A]]): AvroDecoder[A] = new AvroDecoder[A] {
    override def decode(schema: Schema, data: Any): Attempt[A] = f.applyOrElse(schema -> data, (x: (Schema, Any)) => Attempt.error(s"Unexpected '$x' (class: ${x.getClass})"))
  }

  def fail[A](error: String): AvroDecoder[A] = new AvroDecoder[A] {
    override def decode(schema: Schema, data: Any): Attempt[A] = Attempt.error(error)
  }

  implicit val interpreter: AvroAlgebra[AvroDecoder] = new AvroAlgebra[AvroDecoder] with AvroDecoderRecordN {

    override val int: AvroDecoder[Int] = partial { case v: Int => Attempt.success(v) }
    override val string: AvroDecoder[String] = partial { case v: Utf8 => Attempt.success(v.toString) }
    override val bool: AvroDecoder[Boolean] = partial { case v: Boolean => Attempt.success(v) }
    override val float: AvroDecoder[Float] = partial { case v: Float => Attempt.success(v) }
    override val byteArray: AvroDecoder[Array[Byte]] = partial[Array[Byte]] { case v: ByteBuffer => Attempt.success(v.array()) }
    override val double: AvroDecoder[Double] = partial { case v: Double => Attempt.success(v) }
    override val long: AvroDecoder[Long] = partial { case v: Long => Attempt.success(v) }
    override val cnil: AvroDecoder[CNil] = fail("Unable to decode cnil")

    override def bigDecimal(scale: Int, precision: Int): AvroDecoder[BigDecimal] = partial[BigDecimal] { case v: ByteBuffer =>

      val decimalType = LogicalTypes.decimal(precision, scale)
      val decimalConversion = new Conversions.DecimalConversion

      Attempt.fromTry(Try(decimalConversion.fromBytes(v, null, decimalType)))
    }

    override def imap[A, B](fa: AvroDecoder[A])(f: A => B)(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(schema: Schema, data: Any): Attempt[B] = fa.decode(schema, data).map(f)
    }

    override def option[A](from: AvroDecoder[A]): AvroDecoder[Option[A]] = new AvroDecoder[Option[A]] {
      override def decode(schema: Schema, data: Any): Attempt[Option[A]] = data match {
        case null => Attempt.Success(None)
        case x => from.decode(schema, x).map(Some.apply)
      }
    }

    override def list[A](of: AvroDecoder[A]): AvroDecoder[List[A]] =
      partialWithSchema {
        case (s, x: Array[_]) =>
          x.toList.traverse[Attempt, A](y => of.decode(s, y))
        case (s, x: java.util.Collection[_]) =>
          x.asScala.toList.traverse[Attempt, A](y => of.decode(s, y))
      }

    override def set[A](of: AvroDecoder[A]): AvroDecoder[Set[A]] =
      list(of).map(_.toSet)

    override def vector[A](of: AvroDecoder[A]): AvroDecoder[Vector[A]] =
      list(of).map(_.toVector)

    override def seq[A](of: AvroDecoder[A]): AvroDecoder[Seq[A]] =
      list(of).map(_.toSeq)

    override def map[K, V](of: AvroDecoder[V])(mapKey: String => Attempt[K])(contramapKey: K => String): AvroDecoder[Map[K, V]] =
      partialWithSchema { case (schema, x: java.util.Map[_, _]) =>
        x.asScala
          .toMap
          .map { case (k, v) => k.toString -> v }
          .foldRight(Attempt.success(Map.empty): Attempt[Map[K, V]]) { case ((key, value), init) =>
            (mapKey(key), of.decode(schema.getValueType, value), init).mapN { case (k, v, acc) => acc + (k -> v) }
          }
      }

    override def pmap[A, B](fa: AvroDecoder[A])(f: A => Attempt[B])(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(schema: Schema, data: Any): Attempt[B] = fa.decode(schema, data).flatMap(f)
    }

    override def or[A, B](fa: AvroDecoder[A], fb: AvroDecoder[B]): AvroDecoder[Either[A, B]] = {
      partialWithSchema { case (s, el) =>
        def loop(schemas: List[Schema]): Attempt[Either[A, B]] = schemas match {
          case x :: xs => Attempt.or(fa.decode(x, el) orElse fb.decode(x, el), loop(xs))
          case Nil => Attempt.error("Unable to match anything")
        }

        s.getType match {
          case Schema.Type.UNION => loop(s.getTypes.asScala.toList)
          case _ => fa.decode(s, el) orElse fb.decode(s, el)
        }
      }
    }
  }

  implicit def apply[A](implicit A: Avro[A]): AvroDecoder[A] = A.apply[AvroDecoder]


}