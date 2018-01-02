package formulation

import java.nio.ByteBuffer

import org.apache.avro.{Conversions, LogicalTypes}
import org.apache.avro.util.Utf8

import scala.annotation.implicitNotFound
import scala.util.Try

@implicitNotFound(msg = "AvroDecoder[${A}] not found, did you implicitly define Avro[${A}]?")
trait AvroDecoder[A] { self =>
  def decode(data: Any): Attempt[A]

  def map[B](f: A => B): AvroDecoder[B] = new AvroDecoder[B] {
    override def decode(data: Any): Attempt[B] = self.decode(data).map(f)
  }
}

object AvroDecoder {

  import scala.collection.JavaConverters._

  def partial[A](f: PartialFunction[Any, Attempt[A]]): AvroDecoder[A] = new AvroDecoder[A] {
    override def decode(data: Any): Attempt[A] = f.applyOrElse(data, (x: Any) => Attempt.error(s"Unexpected '$x' (class: ${x.getClass})"))
  }

  implicit val interpreter: AvroAlgebra[AvroDecoder] = new AvroAlgebra[AvroDecoder] with AvroDecoderRecordN {

    override val int: AvroDecoder[Int] = partial { case v: Int => Attempt.success(v) }
    override val string: AvroDecoder[String] = partial { case v: Utf8 => Attempt.success(v.toString) }
    override val bool: AvroDecoder[Boolean] =  partial { case v: Boolean => Attempt.success(v) }
    override val float: AvroDecoder[Float] = partial { case v: Float => Attempt.success(v) }
    override val byteArray: AvroDecoder[Array[Byte]] = partial[Array[Byte]] { case v: ByteBuffer => Attempt.success(v.array()) }
    override val double: AvroDecoder[Double] = partial { case v: Double => Attempt.success(v) }
    override val long: AvroDecoder[Long] = partial { case v: Long => Attempt.success(v) }
    override def bigDecimal(scale: Int, precision: Int): AvroDecoder[BigDecimal] = partial[BigDecimal] { case v: ByteBuffer =>

      val decimalType = LogicalTypes.decimal(precision, scale)
      val decimalConversion = new Conversions.DecimalConversion

      Attempt.fromTry(Try(decimalConversion.fromBytes(v, null, decimalType)))
    }

    override def imap[A, B](fa: AvroDecoder[A])(f: A => B)(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(data: Any): Attempt[B] = fa.decode(data).map(f)
    }

    override def option[A](from: AvroDecoder[A]): AvroDecoder[Option[A]] = new AvroDecoder[Option[A]] {
      override def decode(data: Any): Attempt[Option[A]] = data match {
        case null => Attempt.Success(None)
        case x => from.decode(x).map(Some.apply)
      }
    }

    override def list[A](of: AvroDecoder[A]): AvroDecoder[List[A]] =
      partial {
        case x: Array[_] =>
          Traverse.listInstance.traverse[Attempt, Any, A](x.toList)(of.decode)
        case x: java.util.Collection[_] =>
          Traverse.listInstance.traverse[Attempt, Any, A](x.asScala.toList)(of.decode)
      }

    override def set[A](of: AvroDecoder[A]): AvroDecoder[Set[A]] =
      list(of).map(_.toSet)

    override def vector[A](of: AvroDecoder[A]): AvroDecoder[Vector[A]] =
      list(of).map(_.toVector)

    override def seq[A](of: AvroDecoder[A]): AvroDecoder[Seq[A]] =
      list(of).map(_.toSeq)

    override def map[V](of: AvroDecoder[V]): AvroDecoder[Map[String, V]] =
      partial { case x: java.util.Map[_,_] =>
        Traverse.mapInstance[String].traverse[Attempt, Any, V](x.asScala.toMap.map { case (k, v) => k.toString -> v })(of.decode)
      }

    override def pmap[A, B](fa: AvroDecoder[A])(f: A => Attempt[B])(g: B => A): AvroDecoder[B] = new AvroDecoder[B] {
      override def decode(data: Any): Attempt[B] = fa.decode(data).flatMap(f)
    }
  }

  implicit def apply[A](implicit A: Avro[A]): AvroDecoder[A] = A.apply[AvroDecoder]
}