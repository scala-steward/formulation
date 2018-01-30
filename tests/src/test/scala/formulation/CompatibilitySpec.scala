package formulation

import org.apache.avro.Schema
import org.scalatest.{Matchers, WordSpec}

class CompatibilitySpec extends WordSpec with Matchers {

  implicit val generic: Avro[Generic[Int]] = record1("user", "User")(Generic.apply[Int])("bla" -> member(int, _.value))

  val v1: Schema = schema[UserV1]
  val v2: Schema = schema[UserV2]
  val v3: Schema = schema[UserV3]
  val genericSchema: Schema = schema[Generic[Int]]

  "Compatiblity" should {
    "return Full for UserV1 and UserV2 - defaults given" in {
      AvroSchemaCompatibility(schema1 = v1, schema2 = v2) shouldBe AvroSchemaCompatibility.Full
    }

    "return Foward for UserV1 and UserV3 - no defaults" in {
      AvroSchemaCompatibility(schema1 = v1, schema2 = v3) shouldBe AvroSchemaCompatibility.Forward
    }

    "return Backward for UserV1 and UserV3 - no defaults" in {
      AvroSchemaCompatibility(schema1 = v3, schema2 = v1) shouldBe AvroSchemaCompatibility.Backward
    }

    "return NotCompatible for UserV1 and Generic" in {
      AvroSchemaCompatibility(schema1 = v3, schema2 = genericSchema) shouldBe AvroSchemaCompatibility.None
    }

    "work while encoding as V1, we should get the right default values when decoding as V2" in {

      val bytes = encode(UserV1(UserId(1), "Mark", "mail@markdejong.org", "it-so-secret"))

      decode[UserV2](bytes, Some(v1)) shouldBe Right(
        UserV2(UserId(1), "Mark", "mail@markdejong.org", "it-so-secret", None, List("Holland"), BookingProcess.Cancelled(2), 1000))
    }
  }
}

