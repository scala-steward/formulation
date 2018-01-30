package formulation

import com.github.benmanes.caffeine.cache.Caffeine
import formulation.util.{PredefinedSchemaRegistryClient, SchemaEntry}
import org.apache.avro.Schema
import org.scalatest.{Matchers, WordSpec}
import cats.implicits._
import formulation.schemaregistry.scalacache.ScalacacheSchemaRegistryClient

import _root_.scalacache._
import _root_.scalacache.modes.try_._
import _root_.scalacache.caffeine._
import scala.util.Try

class ScalacacheSchemaRegistryClientSpec extends WordSpec with Matchers {

  private val schemas = Caffeine.newBuilder().recordStats().maximumSize(10000L).build[String, Entry[Int]]
  private val ids = Caffeine.newBuilder().recordStats().maximumSize(10000L).build[String, Entry[Schema]]
  private val predefinedClient = PredefinedSchemaRegistryClient.fromAvro[Try](Event.codec)
  private val cachedClient = ScalacacheSchemaRegistryClient(predefinedClient, CaffeineCache(schemas), CaffeineCache(ids))

  "ScalacacheSchemaRegistryClient" should {
    "getIdBySchema - return cached result" in {

      cachedClient.getIdBySchema(schema[Event.Completed]) shouldBe scala.util.Success(Some(1L))
      cachedClient.getIdBySchema(schema[Event.Completed]) shouldBe scala.util.Success(Some(1L))

      schemas.stats().hitCount() shouldBe 1L
      schemas.stats().hitRate() shouldBe 0.5
    }

    "getIdBySchema - return none if it's not at the API" in {
      cachedClient.getIdBySchema(schema[UserV1]) shouldBe scala.util.Success(None)
    }

    "getSchemaById - return cached result" in {

      cachedClient.getSchemaById(1) shouldBe scala.util.Success(Some(schema[Event.Completed]))
      cachedClient.getSchemaById(1) shouldBe scala.util.Success(Some(schema[Event.Completed]))

      ids.stats().hitCount() shouldBe 1L
      ids.stats().hitRate() shouldBe 0.5
    }

    "getSchemaById - return none if it's not at the API" in {
      cachedClient.getSchemaById(4) shouldBe scala.util.Success(None)
    }
  }
}
