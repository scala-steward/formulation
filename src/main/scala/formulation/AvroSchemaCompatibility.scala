package formulation

import org.apache.avro.SchemaCompatibility.SchemaCompatibilityType._
import org.apache.avro.{Schema, SchemaCompatibility}

sealed trait AvroSchemaCompatibility

object AvroSchemaCompatibility {

  final case object NotCompatible extends AvroSchemaCompatibility
  final case object Forward extends AvroSchemaCompatibility
  final case object Backward extends AvroSchemaCompatibility
  final case object Full extends AvroSchemaCompatibility

  def apply(writer: Schema, reader: Schema): AvroSchemaCompatibility =
    (isCompatible(writer)(reader), isCompatible(reader)(writer)) match {
      case (false, true) => Backward
      case (true, false) => Forward
      case (true, true) => Full
      case (false, false) => NotCompatible
    }

  def isReadable(target: Schema, comparison: Schema): Boolean = {
    val compatibility = apply(target, comparison)
    compatibility == Full || compatibility == Backward
  }

  private def isCompatible(target: Schema)(comparison: Schema): Boolean =
    SchemaCompatibility.checkReaderWriterCompatibility(target, comparison).getType == COMPATIBLE
}
