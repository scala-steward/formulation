package formulation.benchmark.util

import cats.MonadError
import formulation.AvroSchemaCompatibility
import formulation.schemaregistry.SchemaRegistryClient
import org.apache.avro.Schema

case class SchemaEntry(id: Int, subject: String, schema: Schema)

class PredefinedSchemaRegistryClient[F[_]](entries: List[SchemaEntry])(implicit F: MonadError[F, Throwable]) extends SchemaRegistryClient[F]() {
  override def getSchemaById(id: Int): F[Option[Schema]] = F.pure(entries.find(_.id == id).map(_.schema))

  override def getIdBySchema(schema: Schema): F[Option[Int]] = F.pure(entries.find(_.schema == schema).map(_.id))

  override def registerSchema(schema: Schema): F[Int] = F.raiseError(new Throwable("Not implemented"))

  override def checkCompatibility(schema: Schema): F[Boolean] = F.raiseError(new Throwable("Not implemented"))

  override def getCompatibilityLevel(subject: String): F[Option[AvroSchemaCompatibility]] = F.raiseError(new Throwable("Not implemented"))

  override def setCompatibilityLevel(subject: String, desired: AvroSchemaCompatibility): F[AvroSchemaCompatibility] = F.raiseError(new Throwable("Not implemented"))
}