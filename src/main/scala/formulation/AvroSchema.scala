package formulation

import org.apache.avro.Schema

import scala.annotation.implicitNotFound

@implicitNotFound(msg = "AvroSchema[${A}] not found, did you implicitly define Avro[${A}]?")
trait AvroSchema[A] {
  def generateSchema: Schema
}

object AvroSchema {

  def create[A](schema: Schema): AvroSchema[A] = new AvroSchema[A] {
    override def generateSchema: Schema = schema
  }

  def by[A, B](fa: AvroSchema[A])(f: B => A): AvroSchema[B] = new AvroSchema[B] {
    override def generateSchema: Schema = fa.generateSchema
  }

  implicit val interpreter: AvroAlgebra[AvroSchema] = new AvroAlgebra[AvroSchema] with AvroSchemaRecordN {

    override val int: AvroSchema[Int] = AvroSchema.create(Schema.create(Schema.Type.INT))

    override val string: AvroSchema[String] = AvroSchema.create(Schema.create(Schema.Type.STRING))

    override val bool: AvroSchema[Boolean] = AvroSchema.create(Schema.create(Schema.Type.BOOLEAN))

    override val float: AvroSchema[Float] = AvroSchema.create(Schema.create(Schema.Type.FLOAT))

    override val byteArray: AvroSchema[Array[Byte]] = AvroSchema.create(Schema.create(Schema.Type.BYTES))

    override val double: AvroSchema[Double] = AvroSchema.create(Schema.create(Schema.Type.DOUBLE))

    override val long: AvroSchema[Long] = AvroSchema.create(Schema.create(Schema.Type.LONG))

    override def bigDecimal(scale: Int, precision: Int): AvroSchema[BigDecimal] = AvroSchema.create(Schema.create(Schema.Type.BYTES))

    override def imap[A, B](fa: AvroSchema[A])(f: A => B)(g: B => A): AvroSchema[B] = by(fa)(g)

    override def option[A](from: AvroSchema[A]): AvroSchema[Option[A]] = AvroSchema.create(Schema.createUnion(Schema.create(Schema.Type.NULL), from.generateSchema))

    override def list[A](of: AvroSchema[A]): AvroSchema[List[A]] = AvroSchema.create(Schema.createArray(of.generateSchema))

    override def pmap[A, B](fa: AvroSchema[A])(f: A => Attempt[B])(g: B => A): AvroSchema[B] = by(fa)(g)

    override def set[A](of: AvroSchema[A]): AvroSchema[Set[A]] = AvroSchema.create(Schema.createArray(of.generateSchema))

    override def vector[A](of: AvroSchema[A]): AvroSchema[Vector[A]] = AvroSchema.create(Schema.createArray(of.generateSchema))

    override def seq[A](of: AvroSchema[A]): AvroSchema[Seq[A]] = AvroSchema.create(Schema.createArray(of.generateSchema))

    override def map[V](value: AvroSchema[V]): AvroSchema[Map[String, V]] = AvroSchema.create(Schema.createMap(value.generateSchema))
  }

  implicit def apply[A](implicit A: Avro[A]): AvroSchema[A] = A.apply[AvroSchema]
}