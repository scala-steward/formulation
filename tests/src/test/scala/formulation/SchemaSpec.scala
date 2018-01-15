package formulation

import org.scalatest.{Matchers, WordSpec}

class SchemaSpec extends WordSpec with Matchers  {

  "AvroSchema" should {
    "generate for Int" in { schema(int) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":"int"}]}"""}
    "generate for Double" in { schema(double) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":"double"}]}"""}
    "generate for Float" in { schema(float) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":"float"}]}"""}
    "generate for String" in { schema(string) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":"string"}]}"""}
    "generate for Bool" in { schema(bool) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":"boolean"}]}"""}
    "generate for BigDecimal" in { schema(bigDecimal(300, 300)) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":{"type":"bytes","logicalType":"decimal","precision":300,"scale":300}}]}"""}
    "generate for Array[Byte]" in { schema(byteArray) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":"bytes"}]}"""}
    "generate for Long" in { schema(long) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":"long"}]}"""}
    "generate for CNil" in { schema(cnil) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":"null"}]}"""}
    "generate for Option[Int]" in { schema(option(int)) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":["null","int"]}]}"""}
    "generate for List[Int]" in { schema(list(int)) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":{"type":"array","items":"int"}}]}"""}
    "generate for Set[Int]" in { schema(set(int)) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":{"type":"array","items":"int"}}]}"""}
    "generate for Vector[Int]" in { schema(vector(int)) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":{"type":"array","items":"int"}}]}"""}
    "generate for Seq[Int]" in { schema(seq(int)) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":{"type":"array","items":"int"}}]}"""}
    "generate for Map[String, Int]" in { schema(map(int)(Right.apply)(identity)) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":{"type":"map","values":"int"}}]}"""}
    "generate for ADT" in { schema(BookingProcess.codec) shouldBe """{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":[{"type":"record","name":"Cancelled","doc":"","fields":[{"name":"disc","type":"int"}]},{"type":"record","name":"DateSelected","doc":"","fields":[{"name":"disc","type":"int"},{"name":"datetime","type":"string"}]},{"type":"record","name":"NotStarted","doc":"","fields":[{"name":"disc","type":"int"}]},"null"]}]}"""}

    def schema[A](avroPart: Avro[A]): String =
      AvroSchema[Generic[A]](Generic.codec(avroPart)).generateSchema.toString

  }
}
