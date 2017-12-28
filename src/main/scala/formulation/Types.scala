package formulation

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import cats._
import org.apache.avro.Schema
import org.apache.avro.Schema.Field
import org.apache.avro.file.DataFileReader
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.util.Utf8

case class Member[F[_], A, B](typeClass: F[A], getter: B => A, defaultValue: Option[A] = None) {
  def mapTypeClass[G[_]](f: F ~> G): Member[G, A, B] = copy(typeClass = f(typeClass))
}


trait AvroAlg[F[_]] extends Invariant[F] {
  val int: F[Int]
  val string: F[String]

  def record1[A, B](namespace: String, name: String)(f: A => B)(paramA: (String, Member[F, A, B])): F[B]
  def record2[A, B, C](namespace: String, name: String)(f: (A, B) => C)(paramA: (String, Member[F, A, C]), paramB: (String, Member[F, B, C])): F[C]
  def record3[A, B, C, D](namespace: String, name: String)(f: (A, B, C) => D)(paramA: (String, Member[F, A, D]), paramB: (String, Member[F, B, D]), paramC: (String, Member[F, C, D])): F[D]

  def record4[A, B, C, D, E](namespace: String, name: String)
                            (f: (A, B, C, D) => E)
                            (paramA: (String, Member[F, A, E]), paramB: (String, Member[F, B, E]), paramC: (String, Member[F, C, E]), paramD: (String, Member[F, D, E])): F[E]
}

trait Avro[A] {
  def apply[F[_] : AvroAlg]: F[A]
}

object Avro {

  def naturalTransformation[G[_] : AvroAlg]: (Avro ~> G) = new (Avro ~> G) {
    override def apply[A](fa: Avro[A]): G[A] = fa.apply[G]
  }

  val int: Avro[Int] = new Avro[Int] {
    override def apply[F[_] : AvroAlg]: F[Int] = implicitly[AvroAlg[F]].int
  }

  val string: Avro[String] = new Avro[String] {
    override def apply[F[_] : AvroAlg]: F[String] = implicitly[AvroAlg[F]].string
  }

  def imap[A, B](fa: Avro[A])(f: A => B)(g: B => A): Avro[B] = new Avro[B] {
    override def apply[F[_] : AvroAlg]: F[B] = implicitly[AvroAlg[F]].imap(fa.apply[F])(f)(g)
  }

  def record1[A, B](namespace: String, name: String)(f: A => B)(paramA: (String, Member[Avro, A, B])): Avro[B] = new Avro[B] {
    override def apply[F[_] : AvroAlg]: F[B] = implicitly[AvroAlg[F]].record1(namespace, name)(f)(paramA._1 -> paramA._2.mapTypeClass(naturalTransformation))
  }

  def record2[A, B, C](namespace: String, name: String)(f: (A,B) => C)(paramA: (String, Member[Avro, A, C]), paramB: (String, Member[Avro, B, C])): Avro[C] = new Avro[C] {
    override def apply[F[_] : AvroAlg]: F[C] = implicitly[AvroAlg[F]].record2(namespace, name)(f)(
      paramA._1 -> paramA._2.mapTypeClass(naturalTransformation),
      paramB._1 -> paramB._2.mapTypeClass(naturalTransformation)
    )
  }

  def record3[A, B, C, D](namespace: String, name: String)(f: (A,B,C) => D)(paramA: (String, Member[Avro, A, D]), paramB: (String, Member[Avro, B, D]), paramC: (String, Member[Avro, C, D])): Avro[D] = new Avro[D] {
    override def apply[F[_] : AvroAlg]: F[D] = implicitly[AvroAlg[F]].record3(namespace, name)(f)(
      paramA._1 -> paramA._2.mapTypeClass(naturalTransformation),
      paramB._1 -> paramB._2.mapTypeClass(naturalTransformation),
      paramC._1 -> paramC._2.mapTypeClass(naturalTransformation)
    )
  }

  def record4[A, B, C, D, E](namespace: String, name: String)
                         (f: (A,B,C,D) => E)
                         (paramA: (String, Member[Avro, A, E]), paramB: (String, Member[Avro, B, E]), paramC: (String, Member[Avro, C, E]), paramD: (String, Member[Avro, D, E])): Avro[E] = new Avro[E] {
    override def apply[F[_] : AvroAlg]: F[E] = implicitly[AvroAlg[F]].record4(namespace, name)(f)(
      paramA._1 -> paramA._2.mapTypeClass(naturalTransformation),
      paramB._1 -> paramB._2.mapTypeClass(naturalTransformation),
      paramC._1 -> paramC._2.mapTypeClass(naturalTransformation),
      paramD._1 -> paramD._2.mapTypeClass(naturalTransformation)
    )
  }


  implicit val invariant: Invariant[Avro] = new Invariant[Avro] {
    override def imap[A, B](fa: Avro[A])(f: A => B)(g: B => A): Avro[B] = Avro.imap(fa)(f)(g)
  }
}

case class Address(street: String, houseNumber: Int)
case class Person(name: String, age: Int, address: Address)
case class Vector4(a: Int, b: Int, c: Int, d: Int)

trait AvroSchema[A] {
  def generateSchema: Schema
}

object AvroSchema {

  import scala.collection.JavaConverters._

  def apply[A](schema: Schema): AvroSchema[A] = new AvroSchema[A] {
    override def generateSchema: Schema = schema
  }

  implicit val contravariant: Contravariant[AvroSchema] = new Contravariant[AvroSchema] {
    override def contramap[A, B](fa: AvroSchema[A])(f: B => A): AvroSchema[B] = AvroSchema(fa.generateSchema)
  }

  implicit val interpreter: AvroAlg[AvroSchema] = new AvroAlg[AvroSchema] {

    override val int: AvroSchema[Int] = AvroSchema(Schema.create(Schema.Type.INT))

    override val string: AvroSchema[String] = AvroSchema(Schema.create(Schema.Type.STRING))



    override def record1[A, B](namespace: String, name: String)(f: A => B)(paramA: (String, Member[AvroSchema, A, B])): AvroSchema[B] =
      AvroSchema(Schema.createRecord(name, "", namespace, false, List(new Field(paramA._1, paramA._2.typeClass.generateSchema, null, null)).asJava))

    override def record2[A, B, C](namespace: String, name: String)(f: (A, B) => C)(paramA: (String, Member[AvroSchema, A, C]), paramB: (String, Member[AvroSchema, B, C])): AvroSchema[C] =
      AvroSchema(
        Schema.createRecord(
          name,
          "",
          namespace,
          false,
          List(
            new Field(paramA._1, paramA._2.typeClass.generateSchema, null, null),
            new Field(paramB._1, paramB._2.typeClass.generateSchema, null, null)
          ).asJava
        )
      )

    override def record3[A, B, C, D](namespace: String, name: String)(f: (A, B, C) => D)(paramA: (String, Member[AvroSchema, A, D]), paramB: (String, Member[AvroSchema, B, D]), paramC: (String, Member[AvroSchema, C, D])): AvroSchema[D] =
      AvroSchema(
        Schema.createRecord(
          name,
          "",
          namespace,
          false,
          List(
            new Field(paramA._1, paramA._2.typeClass.generateSchema, null, null),
            new Field(paramB._1, paramB._2.typeClass.generateSchema, null, null),
            new Field(paramC._1, paramC._2.typeClass.generateSchema, null, null)
          ).asJava
        )
      )

    override def record4[A, B, C, D, E](namespace: String, name: String)(f: (A, B, C, D) => E)(paramA: (String, Member[AvroSchema, A, E]), paramB: (String, Member[AvroSchema, B, E]), paramC: (String, Member[AvroSchema, C, E]), paramD: (String, Member[AvroSchema, D, E])): AvroSchema[E] =
      AvroSchema(
        Schema.createRecord(
          name,
          "",
          namespace,
          false,
          List(
            new Field(paramA._1, paramA._2.typeClass.generateSchema, null, null),
            new Field(paramB._1, paramB._2.typeClass.generateSchema, null, null),
            new Field(paramC._1, paramC._2.typeClass.generateSchema, null, null),
            new Field(paramD._1, paramD._2.typeClass.generateSchema, null, null)
          ).asJava
        )
      )


    override def imap[A, B](fa: AvroSchema[A])(f: A => B)(g: B => A): AvroSchema[B] = contravariant.contramap(fa)(g)
  }

  def apply[A](implicit T: AvroSchema[A]): AvroSchema[A] = T
}

trait AvroEncoder[A] {
  def encode(schema: Schema, value: A): Any
}

object AvroEncoder {

  def apply[A](f: (Schema, A) => Any): AvroEncoder[A] = new AvroEncoder[A] {
    override def encode(schema: Schema, value: A): Any = f(schema, value)
  }

  implicit val interpreter: AvroAlg[AvroEncoder] = new AvroAlg[AvroEncoder] {


    override val string: AvroEncoder[String] = AvroEncoder((_, v) => v)

    override val int: AvroEncoder[Int] = AvroEncoder((_, v) => v)

    override def imap[A, B](fa: AvroEncoder[A])(f: A => B)(g: B => A): AvroEncoder[B] = AvroEncoder((_, v) => g(v))

    override def record1[A, B](namespace: String, name: String)(f: A => B)(paramA: (String, Member[AvroEncoder, A, B])): AvroEncoder[B] = ???

    override def record2[A, B, C](namespace: String, name: String)(f: (A, B) => C)(paramA: (String, Member[AvroEncoder, A, C]), paramB: (String, Member[AvroEncoder, B, C])): AvroEncoder[C] = new AvroEncoder[C] {
      override def encode(schema: Schema, value: C): Any = {
        val record = new GenericData.Record(schema)
        record.put(paramA._1, paramA._2.typeClass.encode(schema.getField(paramA._1).schema(), paramA._2.getter(value)))
        record.put(paramB._1, paramB._2.typeClass.encode(schema.getField(paramB._1).schema(), paramB._2.getter(value)))
        record
      }
    }

    override def record3[A, B, C, D](namespace: String, name: String)(f: (A, B, C) => D)(paramA: (String, Member[AvroEncoder, A, D]), paramB: (String, Member[AvroEncoder, B, D]), paramC: (String, Member[AvroEncoder, C, D])): AvroEncoder[D] = new AvroEncoder[D] {
      override def encode(schema: Schema, value: D): Any = {
        val record = new GenericData.Record(schema)
        record.put(paramA._1, paramA._2.typeClass.encode(schema.getField(paramA._1).schema(), paramA._2.getter(value)))
        record.put(paramB._1, paramB._2.typeClass.encode(schema.getField(paramB._1).schema(), paramB._2.getter(value)))
        record.put(paramC._1, paramC._2.typeClass.encode(schema.getField(paramC._1).schema(), paramC._2.getter(value)))
        record
      }
    }

    override def record4[A, B, C, D, E](namespace: String, name: String)(f: (A, B, C, D) => E)(paramA: (String, Member[AvroEncoder, A, E]), paramB: (String, Member[AvroEncoder, B, E]), paramC: (String, Member[AvroEncoder, C, E]), paramD: (String, Member[AvroEncoder, D, E])): AvroEncoder[E] = ???
  }
}

sealed trait AvroData

object AvroData {
  case class Integer(value: Int) extends AvroData
  case class Str(value: String) extends AvroData
  case class Record(value: GenericRecord) extends AvroData
}

trait AvroDecoder[A] {
  def decode(data: AvroData): Either[String, A]
}

object AvroDecoder {

  def apply[A](f: AvroData => Either[String, A]) = new AvroDecoder[A] {
    override def decode(data: AvroData): Either[String, A] = f(data)
  }

  def partial[A](f: PartialFunction[AvroData, Either[String, A]]) = new AvroDecoder[A] {
    override def decode(data: AvroData): Either[String, A] = f.applyOrElse(data, (x: AvroData) => Left(s"Unexpected $x"))
  }

  def record[A](f: GenericRecord => Either[String, A]) = partial {
    case AvroData.Record(record) => f(record)
  }

  private def toAvroData(anyRef: Any) = anyRef match {
    case x: Int => AvroData.Integer(x)
    case x: String => AvroData.Str(x)
    case x: Utf8 => AvroData.Str(x.toString)
    case x: GenericRecord => AvroData.Record(x)
    case x => sys.error(s"Unrecognized data type: ${x.getClass}")
  }

  implicit val interpreter = new AvroAlg[AvroDecoder] {

    override val int: AvroDecoder[Int] = partial { case AvroData.Integer(v) => Right(v) }
    override val string: AvroDecoder[String] = partial { case AvroData.Str(v) => Right(v) }

    override def record1[A, B](namespace: String, name: String)(f: A => B)(paramA: (String, Member[AvroDecoder, A, B])): AvroDecoder[B] = record { r =>
      for {
        a <- paramA._2.typeClass.decode(toAvroData(r.get(paramA._1)))
      } yield f(a)
    }

    override def record2[A, B, C](namespace: String, name: String)(f: (A, B) => C)(paramA: (String, Member[AvroDecoder, A, C]), paramB: (String, Member[AvroDecoder, B, C])): AvroDecoder[C] =
      record { r =>
        for {
          a <- paramA._2.typeClass.decode(toAvroData(r.get(paramA._1)))
          b <- paramB._2.typeClass.decode(toAvroData(r.get(paramB._1)))
        } yield f(a, b)
      }

    override def record3[A, B, C, D](namespace: String, name: String)(f: (A, B, C) => D)(paramA: (String, Member[AvroDecoder, A, D]), paramB: (String, Member[AvroDecoder, B, D]), paramC: (String, Member[AvroDecoder, C, D])): AvroDecoder[D] =
      record { r =>
        for {
          a <- paramA._2.typeClass.decode(toAvroData(r.get(paramA._1)))
          b <- paramB._2.typeClass.decode(toAvroData(r.get(paramB._1)))
          c <- paramC._2.typeClass.decode(toAvroData(r.get(paramC._1)))
        } yield f(a, b, c)
      }

    override def record4[A, B, C, D, E](namespace: String, name: String)(f: (A, B, C, D) => E)(paramA: (String, Member[AvroDecoder, A, E]), paramB: (String, Member[AvroDecoder, B, E]), paramC: (String, Member[AvroDecoder, C, E]), paramD: (String, Member[AvroDecoder, D, E])): AvroDecoder[E] = ???

    override def imap[A, B](fa: AvroDecoder[A])(f: A => B)(g: B => A): AvroDecoder[B] = ???
  }
}

object Main extends App {

  import Avro._

  def encode[A](value: A)(implicit R: AvroEncoder[A], S: AvroSchema[A]): Array[Byte] = {
    val os = new ByteArrayOutputStream()
    val schema = S.generateSchema
    val dataWriter = new GenericDatumWriter[GenericRecord](schema)
    val encoder = EncoderFactory.get().binaryEncoder(os, null)

    dataWriter.write(R.encode(schema, value).asInstanceOf[GenericRecord], encoder)
    encoder.flush()


    os.toByteArray
  }

  def decode[A](bytes: Array[Byte], writerSchema: Option[Schema] = None, readerSchema: Option[Schema] = None)(implicit R: AvroDecoder[A], S: AvroSchema[A]): Either[String, A] = {

    val schema = S.generateSchema
    val wSchema = writerSchema.getOrElse(schema)
    val rSchema = readerSchema.getOrElse(schema)
    val datumReader = new GenericDatumReader[GenericRecord](wSchema, rSchema)
    val in = new ByteArrayInputStream(bytes)
    val binDecoder = DecoderFactory.get().binaryDecoder(in, null)
    val record = datumReader.read(null, binDecoder)

    R.decode(AvroData.Record(record))
  }

  val address = record2("forma", "Address")(Address.apply)("street" -> Member(string, _.street), "houseNumber" -> Member(int, _.houseNumber))
  def person = record3("forma", "Person")(Person.apply)("name" -> Member(string, _.name), "age" -> Member(int, _.age), "address" -> Member(address, _.address))

//  val schema = person.apply[ToSchema].generateSchema

  implicit val encoder = person.apply[AvroEncoder]
  implicit val decoder = person.apply[AvroDecoder]
  implicit val toSchema = person.apply[AvroSchema]

  val bytes = encode(Person("Mark", 31, Address("Westerdijk", 4)))
  val result = decode(bytes)

  println(result)
}