package formulation.util

import cats.MonadError
import formulation.{Avro, AvroSchema, AvroSchemaCompatibility}
import formulation.schemaregistry.SchemaRegistryClient
import org.apache.avro.Schema
import scala.collection.JavaConverters._

sealed class PredefinedSchemaRegistryClient[F[_]] private (entries: List[SchemaEntry])(implicit F: MonadError[F, Throwable]) extends SchemaRegistryClient[F]() {
  override def getSchemaById(id: Int): F[Option[Schema]] = F.pure(entries.find(_.id == id).map(_.schema))

  override def getIdBySchema(schema: Schema): F[Option[Int]] = F.pure(entries.find(_.schema == schema).map(_.id))

  override def registerSchema(schema: Schema): F[Int] = F.raiseError(new Throwable("Not implemented"))

  override def checkCompatibility(schema: Schema): F[Boolean] = F.raiseError(new Throwable("Not implemented"))

  override def getCompatibilityLevel(subject: String): F[Option[AvroSchemaCompatibility]] = F.raiseError(new Throwable("Not implemented"))

  override def setCompatibilityLevel(subject: String, desired: AvroSchemaCompatibility): F[AvroSchemaCompatibility] = F.raiseError(new Throwable("Not implemented"))
}

object PredefinedSchemaRegistryClient {
  def fromAvro[F[_]](definition: Avro[_], defintions: Avro[_]*)(implicit F: MonadError[F, Throwable]): SchemaRegistryClient[F] = {
    val schemas = (definition :: defintions.toList).map(_.apply[AvroSchema].schema)
    val (_, entries) = schemas.foldLeft(0 -> List.empty[SchemaEntry]) { case ((offset, e), s) =>
      val ne = s.getType match {
        case Schema.Type.UNION =>
          s.getTypes.asScala.toList.zipWithIndex.map { case (ss, id) => SchemaEntry(offset + id, ss.getFullName, ss) }
        case Schema.Type.RECORD => SchemaEntry(offset, s.getFullName, s) :: Nil
        case _ => sys.error("Not supported")
      }

      (ne.maxBy(_.id).id + 1) -> (e ++ ne)
    }

    new PredefinedSchemaRegistryClient[F](entries)
  }
}
