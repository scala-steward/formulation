package formulation.akkaserializer

import akka.serialization.Serializer
import formulation._
import formulation.schemaregistry.SchemaRegistry
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.{BinaryDecoder, BinaryEncoder}

import scala.reflect.ClassTag
import scala.util.Try

abstract class FormulationAkkaSerializer[A <: AnyRef : ClassTag : AvroSchema : AvroEncoder : AvroDecoder](sr: SchemaRegistry[Try]) extends Serializer {
  def identifier: Int = 1337
  def includeManifest: Boolean = false

  private var binaryEncoder: Option[BinaryEncoder] = Option.empty
  private var binaryDecoder: Option[BinaryDecoder] = Option.empty

  def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: A =>
      val res = sr.kleisliEncode.run(AvroEncodeContext(x, binaryEncoder)).orThrow
      binaryEncoder = res.binaryEncoder
      res.entity
    case other =>
      throw new Throwable(s"Could not encode $other, didn't match serializer")
  }
  def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val res = sr.kleisliDecode[A].run(AvroDecodeContext(bytes, binaryDecoder)).orThrow
    binaryDecoder = res.binaryDecoder
    res.entity match {
      case Right(value) => value
      case Left(AvroDecodeFailure.Errors(record, errors)) => throw AvroDecodeException(record, errors)
      case Left(AvroDecodeFailure.Exception(ex)) => throw ex
      case Left(AvroDecodeFailure.Noop) => throw new Throwable("impossible")
    }
  }

  private implicit class TryOps[Z](val t: Try[Z]) {
    def orThrow: Z = t match {
      case scala.util.Success(v) => v
      case scala.util.Failure(ex) => throw ex
    }
  }
}