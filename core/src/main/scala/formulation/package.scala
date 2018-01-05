import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

import scala.util.control.NonFatal

package object formulation extends AvroDsl {

  private val encoderFactory = EncoderFactory.get()
  private val decoderFactory = DecoderFactory.get()

  def member[A, B](avro: Avro[A], getter: B => A, defaultValue: Option[A] = None): Member[Avro, A, B] =
    Member[Avro, A, B](avro, getter, defaultValue.map(v => avro.apply[AvroDefaultValuePrinter].print(v)))

  def encode[A](value: A)(implicit R: AvroEncoder[A], S: AvroSchema[A]): Array[Byte] = {
    val os = new ByteArrayOutputStream()

    try {
      val schema = S.generateSchema
      val dataWriter = new GenericDatumWriter[GenericRecord](schema)
      val encoder = encoderFactory.directBinaryEncoder(os, null)

      dataWriter.write(R.encode(schema, value).asInstanceOf[GenericRecord], encoder)

      encoder.flush()

      os.toByteArray
    }
    finally {
      os.close()
    }
  }

  def decode[A](bytes: Array[Byte], writerSchema: Option[Schema] = None, readerSchema: Option[Schema] = None)
               (implicit R: AvroDecoder[A], S: AvroSchema[A]): Attempt[A] = {

    val in = new ByteArrayInputStream(bytes)

    try {
      def schema = S.generateSchema
      val wSchema = writerSchema.getOrElse(schema)
      val rSchema = readerSchema.getOrElse(schema)
      val datumReader = new GenericDatumReader[GenericRecord](wSchema, rSchema)
      val binDecoder = decoderFactory.directBinaryDecoder(in, null)

      val record = datumReader.read(null, binDecoder)

      R.decode(record)
    }
    catch {
      case NonFatal(ex) => Attempt.exception(DecodeError(ex))
    }
    finally {
      in.close()
    }
  }
}
