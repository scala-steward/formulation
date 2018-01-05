package formulation

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}

class DefaultSchemaSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks with ArbitraryHelpers {

  def prettyPrint[A](value: A, avro: Avro[A]): String = {
    avro.apply[AvroDefaultValuePrinter].print(value).toString
  }

  def genericRecordSchema(t: String, default: String) =
    s"""{"type":"record","name":"Generic","namespace":"formulation","doc":"","fields":[{"name":"value","type":$t,"default":$default}]}"""

  "AvroSchema defaults" should {
    "generate defaults for Int" in {
      forAll { (v: Int) =>
        schema(int, v) shouldBe genericRecordSchema(""""int"""", v.toString)
      }
    }
    "generate defaults for Double" in {
      forAll { (v: Double) =>
        schema(double, v) shouldBe genericRecordSchema(""""double"""", v.toString)
      }
    }
    "generate defaults for Float" in {
      forAll { (v: Float) =>
        schema(float, v) shouldBe genericRecordSchema(""""float"""", prettyPrint(v, float))
      }
    }

    "generate defaults for String" in {
      forAll { (v: String) =>
        schema(string, v) shouldBe genericRecordSchema(""""string"""", s""""$v"""")
      }
    }

    "generate defaults for Bool" in {
      forAll { (v: Boolean) =>
        schema(bool, v) shouldBe genericRecordSchema(""""boolean"""", v.toString)
      }
    }
    "generate defaults for Long" in {
      forAll { (v: Long) =>
        schema(long, v) shouldBe genericRecordSchema(""""long"""", v.toString)
      }
    }

// It seems with the default configuration (we cannot configure the Schema.JsonGenerator) this is problematic, covered these cases by CompatibilitySpec though
//    "generate defaults for BigDecimal" in {
//      forAll { (v: BigDecimal) =>
//        schema(bigDecimal(300, 300), v) shouldBe genericRecordSchema("""{"type":"bytes","logicalType":"decimal","precision":300,"scale":300}""", prettyPrint(v, bigDecimal(300, 300)))
//      }
//    }
//    "generate defaults for Array[Byte]" in {
//      forAll { (v: Array[Byte]) =>
//        schema(byteArray, v) shouldBe genericRecordSchema(""""bytes"""", prettyPrint(v, byteArray))
//      }
//    }
    "generate defaults for Option[Int]" in {
      forAll { (v: Option[Int]) =>
        schema(option(int), v) shouldBe genericRecordSchema("""["null","int"]""", prettyPrint(v, option(int)))
      }

    }
    "generate defaults for List[Int]" in {
      forAll { (v: List[Int]) =>
        schema(list(int), v) shouldBe genericRecordSchema("""{"type":"array","items":"int"}""", prettyPrint(v, list(int)))
      }

    }
    "generate defaults for Set[Int]" in {
      forAll { (v: Set[Int]) =>
        schema(set(int), v) shouldBe genericRecordSchema("""{"type":"array","items":"int"}""", prettyPrint(v, set(int)))
      }
    }
    "generate defaults for Vector[Int]" in {
      forAll { (v: Vector[Int]) =>
        schema(vector(int), v) shouldBe genericRecordSchema("""{"type":"array","items":"int"}""", prettyPrint(v, vector(int)))
      }

    }
    "generate defaults for Seq[Int]" in {
      forAll { (v: Seq[Int]) =>
        schema(seq(int), v) shouldBe  genericRecordSchema("""{"type":"array","items":"int"}""", prettyPrint(v, seq(int)))
      }

    }

    "generate defaults for Map[String, Int]" in {
      forAll(mapGen) { v =>
        schema(map(int)(Attempt.success)(identity), v) shouldBe
          genericRecordSchema("""{"type":"map","values":"int"}""", prettyPrint(v, map(int)(Attempt.success)(identity)))
      }
    }
    "generate defaults for ADT" in {
      forAll { (v: BookingProcess) =>
        schema(BookingProcess.codec, v) shouldBe
          genericRecordSchema("""[{"type":"record","name":"Cancelled","doc":"","fields":[{"name":"disc","type":"int"}]},{"type":"record","name":"DateSelected","doc":"","fields":[{"name":"disc","type":"int"},{"name":"datetime","type":"string"}]},{"type":"record","name":"NotStarted","doc":"","fields":[{"name":"disc","type":"int"}]},"null"]""", prettyPrint(v, BookingProcess.codec))
      }
    }

    def schema[A](avroPart: Avro[A], default: A): String =
      AvroSchema[Generic[A]](Generic.codec(avroPart, Some(default))).generateSchema.toString
  }
}
