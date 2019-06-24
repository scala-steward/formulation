package org.apache.avro

import com.fasterxml.jackson.databind.JsonNode

final class FormulationField (
  name: String,
  schema: Schema,
  doc: String,
  defaultValue: JsonNode,
  order: Schema.Field.Order) extends Schema.Field(name, schema, doc, defaultValue, false, order)
