package formulation.akkastreams

import formulation.AvroDecodeError
import org.apache.avro.generic.GenericRecord

final case class AvroDecodeException(record: GenericRecord, errors: List[AvroDecodeError]) extends Throwable(s"Failed to decode a `${record.getSchema.getFullName}`")