package formulation

import java.time.Instant
import java.util.UUID

import formulation.akkaserializer.FormulationAkkaSerializer
import formulation.schemaregistry.SchemaRegistry
import formulation.util.PredefinedSchemaRegistryClient
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}
import cats.implicits._

import scala.util.Try

class AkkaSerializerSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks with ArbitraryHelpers {

  class EventSerializer extends FormulationAkkaSerializer[Event](11, SchemaRegistry(PredefinedSchemaRegistryClient.fromAvro[Try](Event.codec)))
  class UserEventV1Serializer extends FormulationAkkaSerializer[UserEventV1](12, SchemaRegistry(PredefinedSchemaRegistryClient.fromAvro[Try](UserEventV1.avro)))
  class UserEventV2Serializer extends FormulationAkkaSerializer[UserEventV2](13, SchemaRegistry(PredefinedSchemaRegistryClient.fromAvro[Try](UserEventV1.avro, UserEventV2.avro)))

  val eventSerializer = new EventSerializer
  val userEventV1Serializer = new UserEventV1Serializer
  val userEventV2Serializer = new UserEventV2Serializer

  "Akka serializer" should {
    "symmetric encode/decode" in {
      forAll { (e: Event) =>
        eventSerializer.fromBinary(eventSerializer.toBinary(e), None) shouldBe e
      }
    }

    "support : Remove event class and ignore events" in {
      val bytes = userEventV1Serializer.toBinary(UserEventV1.Deactivated(UUID.randomUUID(), Instant.now()))
      val res = userEventV2Serializer.fromBinary(bytes, None)

      res shouldBe AvroDecodeSkipReason.NotMemberOfUnion(schema[UserEventV1.Deactivated], schema[UserEventV2])
    }

    "support : Rename and remove fields " in {

      AvroSchemaCompatibility(schema[UserEventV1.Registered], schema[UserEventV2.Registered]) shouldBe AvroSchemaCompatibility.Backward

      val eventV1 = UserEventV1.Registered(UUID.randomUUID(), "Mark", "mark@nonsense.com", "no-so-secret", Instant.now())
      val bytes = userEventV1Serializer.toBinary(eventV1)
      val res = userEventV2Serializer.fromBinary(bytes, None)

      // we renamed email -> emailAddress in V2
      // we removed the password field in V2
      res shouldBe UserEventV2.Registered(eventV1.userId, eventV1.username, eventV1.email, eventV1.timestamp)
    }

    "support : Add fields" in {
      val eventV1 = UserEventV1.Activated(UUID.randomUUID(), Instant.now())
      val bytes = userEventV1Serializer.toBinary(eventV1)
      val res = userEventV2Serializer.fromBinary(bytes, None)

      res shouldBe UserEventV2.Activated(eventV1.userId, "some-payload", eventV1.timestamp)
    }
  }

}
