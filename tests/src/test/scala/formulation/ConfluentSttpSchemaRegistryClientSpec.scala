package formulation

import cats.implicits._
import com.softwaremill.sttp.TryHttpURLConnectionBackend
import formulation.schemaregistry.SchemaRegistryClient
import formulation.schemaregistry.confluent.sttp.ConfluentSttpSchemaRegistryClient
import org.scalatest.{Matchers, WordSpec}

import scala.util.Success

class ConfluentSttpSchemaRegistryClientSpec extends WordSpec with Matchers {

  private val catsSttpBackend = TryHttpURLConnectionBackend()
  private val client = SchemaRegistryClient.lruCached(ConfluentSttpSchemaRegistryClient("http://localhost:8081", catsSttpBackend), 2000)

  "ConfluentSttpSchemaRegistryClientSpec" should {
    "register successfully" in {
      client.registerSchema(schema[UserV1]) shouldBe Success(1)
    }
    "register is idempotent" in {
      client.registerSchema(schema[UserV1]) shouldBe Success(1)
    }
    "check compatibility correctly: user v1 should be compatible with user v4" in {
      client.checkCompatibility(schema[UserV4]) shouldBe Success(true)
    }
    "get id by a given schema" in {
      client.getIdBySchema(schema[UserV1]) shouldBe Success(Some(1))
    }
    "get no id by a given schema" in {
      client.getIdBySchema(schema[UserV2]) shouldBe Success(None)
    }
    "get a schema by a given id" in {
      client.getSchemaById(1) shouldBe Success(Some(schema[UserV1]))
    }
    "get no schema by a given id" in {
      client.getSchemaById(2) shouldBe Success(None)
    }
    "get compatibility level should get None" in {
      client.getCompatibilityLevel("user.NotExisting") shouldBe Success(None)
    }
    "set compatibility level should get Some(Full)" in {
      (client.setCompatibilityLevel("user.User2", AvroSchemaCompatibility.Full) >> client.getCompatibilityLevel("user.User2")) shouldBe Success(Some(AvroSchemaCompatibility.Full))
    }
  }
}
