package formulation

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

sealed abstract class Color(val repr: String)

object Color {
  case object Black extends Color("black")
  case object White extends Color("white")
  case object Orange extends Color("orange")

  val all: Set[Color] = Set(Black, White, Orange)

  implicit val enum: Enum[Color] = Enum(all)(_.repr)
}

trait Enum[A] {
  val allValues: Set[A]
  def asString(value: A): String
}

object Enum {
  def apply[A](values: Set[A])(stringify: A => String): Enum[A] = new Enum[A] {
    override val allValues: Set[A] = values
    override def asString(value: A): String = stringify(value)
  }
}

case class Address(street: String, houseNumber: Int, countries: List[String])
case class Person(name: String, favoriteColor: Color, address: Address, city: Option[String])

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

  implicit val address: Avro[Address] = record3("forma", "Address")(Address.apply)(
    "street" -> Member(string, _.street),
    "houseNumber" -> Member(int, _.houseNumber),
    "countries" -> Member(list(string), _.countries, Some(Nil))
  )

  def enum[A](implicit E: Enum[A]) =
    pmap(string)(str => E.allValues.find(x => E.asString(x) == str).fold[Either[String, A]](Left(s"Value $str not found"))(Right.apply))(E.asString)

  implicit val person: Avro[Person] = record4("forma", "Person")(Person.apply)(
    "name" -> Member(string, _.name),
    "favoriteColor" -> Member(enum[Color], _.favoriteColor),
    "address" -> Member(address, _.address),
    "city" -> Member(option(string), _.city)
  )

  //  println(AvroSchema[Person].generateSchema.toString(true))

  println(decode[Person](encode(Person("Mark", Color.Orange, Address("Westerdijk", 4, List("Netherlands", "Belgium")), Some("Utrecht")))))
}