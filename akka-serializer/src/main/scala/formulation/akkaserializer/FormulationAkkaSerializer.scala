package formulation.akkaserializer

import akka.serialization.Serializer
import formulation._
import formulation.schemaregistry.SchemaRegistry
import org.apache.avro.io.{BinaryDecoder, BinaryEncoder}

import scala.reflect.ClassTag
import scala.util.Try

abstract class FormulationAkkaSerializer[A <: AnyRef : ClassTag : AvroSchema : AvroEncoder : AvroDecoder](id: Int, sr: SchemaRegistry[Try]) extends Serializer {
  final def identifier: Int = id
  final def includeManifest: Boolean = false

  private var binaryEncoder: Option[BinaryEncoder] = Option.empty
  private var binaryDecoder: Option[BinaryDecoder] = Option.empty

  final def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: A =>
      val res = sr.kleisliEncode.run(AvroEncodeContext(x, binaryEncoder)).get
      binaryEncoder = res.binaryEncoder
      res.entity.payload
    case other =>
      sys.error(s"Could not encode $other, didn't match serializer")
  }

  final def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val res = sr.kleisliDecode[A].run(AvroDecodeContext(bytes, binaryDecoder)).get
    binaryDecoder = res.binaryDecoder
    res.entity match {
      case Right(value) => value
      case Left(AvroDecodeFailure.Errors(record, errors)) => throw AvroDecodeException(record, errors)
      case Left(AvroDecodeFailure.Exception(ex)) => throw ex
      case Left(AvroDecodeFailure.Skip(x)) => x
    }
  }
}