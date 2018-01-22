package formulation

import formulation.akkaserializer.FormulationAkkaSerializer
import formulation.schemaregistry.SchemaRegistry
import formulation.util.{PredefinedSchemaRegistryClient, SchemaEntry}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}

import cats.implicits._
import scala.util.Try

class AkkaSerializerSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks with ArbitraryHelpers {

  val predefinedClient = new PredefinedSchemaRegistryClient[Try](
    List(
      SchemaEntry(1, "event.Completed", schema[Event.Completed]),
      SchemaEntry(2, "event.Failed", schema[Event.Failed]),
      SchemaEntry(3, "event.Started", schema[Event.Started])
    )
  )

  class EventSerializer extends FormulationAkkaSerializer[Event](SchemaRegistry(predefinedClient))

  val serializer = new EventSerializer

  "Akka serializer" should {
    "symmertically encode/decode" in {
      forAll { (e: Event) =>
        serializer.fromBinary(serializer.toBinary(e), None) shouldBe e
      }
    }
  }

}
