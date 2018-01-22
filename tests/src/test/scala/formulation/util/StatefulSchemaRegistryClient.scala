package formulation.util

import cats.Monad
import cats.data.StateT
import cats.implicits._
import formulation.AvroSchemaCompatibility
import formulation.schemaregistry.SchemaRegistryClient
import org.apache.avro.Schema

case class SchemaEntry(id: Int, subject: String, schema: Schema)
case class SchemaRegistryState(compatLevels: Map[String, AvroSchemaCompatibility], entries: List[SchemaEntry])

class StatefulSchemaRegistryClient[F[_] : Monad] extends SchemaRegistryClient[StateT[F, SchemaRegistryState, ?]] {
  override def getSchemaById(id: Int): StateT[F, SchemaRegistryState, Option[Schema]] =
    StateT.inspect[F, SchemaRegistryState, Option[Schema]](_.entries.find(_.id == id).map(_.schema))

  override def getIdBySchema(schema: Schema): StateT[F, SchemaRegistryState, Option[Int]] =
    StateT.inspect[F, SchemaRegistryState, Option[Int]](_.entries.find(x => x.schema == schema && x.subject == schema.getFullName).map(_.id))

  override def registerSchema(schema: Schema): StateT[F, SchemaRegistryState, Int] =
    for {
      existingId <- StateT.inspect[F, SchemaRegistryState, Option[Int]](_.entries.find(_.schema == schema).map(_.id))
      id <- existingId match {
        case Some(id) => StateT.pure[F, SchemaRegistryState, Int](id)
        case None =>
          for {
            maxId <- StateT.inspect[F, SchemaRegistryState, Int](x => if (x.entries.nonEmpty) x.entries.maxBy(_.id).id else 0)
            _ <- StateT.modify[F, SchemaRegistryState](sr => sr.copy(entries = SchemaEntry(maxId + 1, schema.getFullName, schema) :: sr.entries))
          } yield maxId + 1
      }
    } yield id

  override def checkCompatibility(schema: Schema): StateT[F, SchemaRegistryState, Boolean] =
    StateT.inspect[F, SchemaRegistryState, Boolean](_.entries.filter(_.subject == schema.getFullName).forall(y => AvroSchemaCompatibility(y.schema, schema) == AvroSchemaCompatibility.Full))

  override def getCompatibilityLevel(subject: String): StateT[F, SchemaRegistryState, Option[AvroSchemaCompatibility]] =
    StateT.inspect[F, SchemaRegistryState, Option[AvroSchemaCompatibility]](_.compatLevels.get(subject))

  override def setCompatibilityLevel(subject: String, desired: AvroSchemaCompatibility): StateT[F, SchemaRegistryState, AvroSchemaCompatibility] =
    StateT.modify[F, SchemaRegistryState](sr => sr.copy(compatLevels = sr.compatLevels + (subject -> desired))) >> StateT.pure(desired)
}
