package formulation

import cats.Contravariant
import org.apache.avro.Schema

trait AvroSchema[A] {
  def generateSchema: Schema
}

object AvroSchema {

  def create[A](schema: Schema): AvroSchema[A] = new AvroSchema[A] {
    override def generateSchema: Schema = schema
  }

  implicit val contravariant: Contravariant[AvroSchema] = new Contravariant[AvroSchema] {
    override def contramap[A, B](fa: AvroSchema[A])(f: B => A): AvroSchema[B] = AvroSchema.create(fa.generateSchema)
  }

  implicit val interpreter: AvroAlgebra[AvroSchema] = new AvroAlgebra[AvroSchema] with AvroSchemaRecordN {

    override val int: AvroSchema[Int] = AvroSchema.create(Schema.create(Schema.Type.INT))

    override val string: AvroSchema[String] = AvroSchema.create(Schema.create(Schema.Type.STRING))

    override def imap[A, B](fa: AvroSchema[A])(f: A => B)(g: B => A): AvroSchema[B] = contravariant.contramap(fa)(g)

    override def option[A](from: AvroSchema[A]): AvroSchema[Option[A]] = AvroSchema.create(Schema.createUnion(Schema.create(Schema.Type.NULL), from.generateSchema))

    override def list[A](of: AvroSchema[A]): AvroSchema[List[A]] = AvroSchema.create(Schema.createArray(of.generateSchema))

    override def pmap[A, B](fa: AvroSchema[A])(f: A => Either[String, B])(g: B => A): AvroSchema[B] = contravariant.contramap(fa)(g)
  }

  implicit def apply[A](implicit A: Avro[A]): AvroSchema[A] = A.apply[AvroSchema]
}