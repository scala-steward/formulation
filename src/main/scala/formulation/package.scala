import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

import scala.util.control.NonFatal

package object formulation extends AvroDsl {

  def encode[A](value: A)(implicit R: AvroEncoder[A], S: AvroSchema[A]): Array[Byte] = {
    val os = new ByteArrayOutputStream()

    try {
      val schema = S.generateSchema
      val dataWriter = new GenericDatumWriter[GenericRecord](schema)
      val encoder = EncoderFactory.get().binaryEncoder(os, null)

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
      val schema = S.generateSchema
      val wSchema = writerSchema.getOrElse(schema)
      val rSchema = readerSchema.getOrElse(schema)
      val datumReader = new GenericDatumReader[GenericRecord](wSchema, rSchema)
      val binDecoder = DecoderFactory.get().binaryDecoder(in, null)

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
