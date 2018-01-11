package formulation

import org.apache.avro.SchemaCompatibility.SchemaCompatibilityType._
import org.apache.avro.{Schema, SchemaCompatibility}

sealed abstract class AvroSchemaCompatibility(val repr: String)

object AvroSchemaCompatibility {

  final case object None extends AvroSchemaCompatibility("NONE")
  final case object Forward extends AvroSchemaCompatibility("FORWARD")
  final case object Backward extends AvroSchemaCompatibility("BACKWARD")
  final case object Full extends AvroSchemaCompatibility("FULL")

  val all = Set(None, Forward, Backward, Full)

  def apply(writer: Schema, reader: Schema): AvroSchemaCompatibility =
    (isCompatible(writer)(reader), isCompatible(reader)(writer)) match {
      case (false, true) => Backward
      case (true, false) => Forward
      case (true, true) => Full
      case (false, false) => None
    }

  private def isCompatible(target: Schema)(comparison: Schema): Boolean =
    SchemaCompatibility.checkReaderWriterCompatibility(target, comparison).getType == COMPATIBLE
}
