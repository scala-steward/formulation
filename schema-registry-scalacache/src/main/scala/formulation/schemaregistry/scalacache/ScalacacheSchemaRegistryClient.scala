package formulation.schemaregistry.scalacache

import formulation.AvroSchemaCompatibility
import formulation.schemaregistry.SchemaRegistryClient
import org.apache.avro.{Schema, SchemaNormalization}

import scala.concurrent.duration.Duration
import scalacache.{Cache, Mode}

sealed class ScalacacheSchemaRegistryClient[F[_]] private (underlying: SchemaRegistryClient[F], schemas: Cache[Int], ids: Cache[Schema])(implicit mode: Mode[F]) extends SchemaRegistryClient[F] {

  override def getSchemaById(id: Int): F[Option[Schema]] =
    ids.cache(id)(None)(underlying.getSchemaById(id))

  override def getIdBySchema(schema: Schema): F[Option[Int]] =
    schemas.cache(SchemaNormalization.parsingFingerprint64(schema))(None)(underlying.getIdBySchema(schema))

  override def registerSchema(schema: Schema): F[Int] =
    underlying.registerSchema(schema)

  override def checkCompatibility(schema: Schema): F[Boolean] = underlying.checkCompatibility(schema)

  override def getCompatibilityLevel(subject: String): F[Option[AvroSchemaCompatibility]] = underlying.getCompatibilityLevel(subject)

  override def setCompatibilityLevel(subject: String, desired: AvroSchemaCompatibility): F[AvroSchemaCompatibility] = underlying.setCompatibilityLevel(subject, desired)

  private implicit class CacheOps[V](val cache: Cache[V]) {
    def cache(keyParts: Any*)(ttl: Option[Duration])(f: => F[Option[V]]): F[Option[V]] = mode.M.flatMap(cache.get[F](keyParts)) {
      case Some(v) => mode.M.pure(Some(v))
      case None => mode.M.flatMap(f) {
        case Some(vv) => mode.M.map(cache.put[F](keyParts)(vv, ttl))(_ => Some(vv))
        case None => mode.M.pure(None)
      }
    }
  }
}

object ScalacacheSchemaRegistryClient {
  def apply[F[_] : Mode](underlying: SchemaRegistryClient[F], schemas: Cache[Int], ids: Cache[Schema]): SchemaRegistryClient[F] =
    new ScalacacheSchemaRegistryClient[F](underlying, schemas, ids)
}
