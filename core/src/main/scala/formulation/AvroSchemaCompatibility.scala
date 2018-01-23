package formulation

import org.apache.avro.SchemaCompatibility.SchemaCompatibilityType._
import org.apache.avro.{Schema, SchemaCompatibility}

sealed abstract class AvroSchemaCompatibility(val repr: String)

object AvroSchemaCompatibility {

  /**
    * No compatibility
    */
  final case object None extends AvroSchemaCompatibility("NONE")

  /**
    * Forward compatibility: Older code can read data that was written by newer code.
    */
  final case object Forward extends AvroSchemaCompatibility("FORWARD")

  /**
    * Backward compatibility: Newer code can read data that was written by older code.
    */
  final case object Backward extends AvroSchemaCompatibility("BACKWARD")

  /**
    * Full compatibility: Older code can read code which has been written by new code, but also new code can read data which has been written by older code.
    */
  final case object Full extends AvroSchemaCompatibility("FULL")

  val all = Set(None, Forward, Backward, Full)

  def apply(schema1: Schema, schema2: Schema): AvroSchemaCompatibility =
    (isCompatible(schema1)(schema2), isCompatible(schema2)(schema1)) match {
      case (false, true) => Backward
      case (true, false) => Forward
      case (true, true) => Full
      case (false, false) => None
    }

  private def isCompatible(reader: Schema)(writer: Schema): Boolean =
    SchemaCompatibility.checkReaderWriterCompatibility(reader, writer).getType == COMPATIBLE
}
