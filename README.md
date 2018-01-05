formulation
---


[![Build Status](https://api.travis-ci.org/vectos/formulation.svg)](https://travis-ci.org/vectos/formulation)
[![codecov.io](http://codecov.io/github/vectos/formulation/coverage.svg?branch=master)](http://codecov.io/github/vectos/formulation?branch=master)

_formulation_ is a EDSL (embedded domain specific language) for describing Avro data types. It

Why use Avro?

- Space and network efficience thanks to a reduced payload.
- Schema evolution intelligence and compatibility enforcing by using a schema registry (which also forces you to centralise schema storage).

Why use Formulation?

- _Expressive_ - It supports the most primitive data types in Scala and allows you to `imap` or `pmap` them
- _Define data types by hand_ - Avro4s derives schema's, encoders and decoders "magically". While this is nice, it can become unwieldy when you have a nested type graph. I believe it's better to explicitly map your data types, so the cognitive process of defining a schema is part of your job instead of a magic macro. This will become important when you want to enforce full compatibility of your schema's
- _Concise_ - The combinators `imap` and `pmap` makes it easy to introduce support for new data types, while this is verbose in Avro4s


### Supported primitives


| Type                      | Combinator           | Remark                                                                        |
| --------------------------|----------------------|-------------------------------------------------------------------------------|
| `Int`                     | `int`                |                                                                               |
| `String`                  | `string`             |                                                                               |
| `Float`                   | `float`              |                                                                               |
| `Double`                  | `double`             |                                                                               |
| `Long`                    | `long`               |                                                                               |
| `Option[A]`               | `option`             |                                                                               |
| `Vector[A]`               | `vector`             |                                                                               |
| `Map[String, A]`          | `map`                | You can contramap/map the key `String`                                        |
| `List[A]`                 | `list`               |                                                                               |
| `Set[A]`                  | `set`                |                                                                               |
| `Seq[A]`                  | `seq`                |                                                                               |
| `Either[L, R]`            | `or`                 |                                                                               |
| `Array[Byte]`             | `byteArray`          | It's encoded as bytes (`ByteBuffer`)                                          |
| `BigDecimal`              | `bigDecimal`         | Uses `byteArray` under the hood, it's encoded as bytes                        |
| `java.util.UUID`          | `uuid`               | Uses `string` under the hood using                                            |
| `java.time.Instant`       | `instant`            | Uses `long` under the hood using                                              |
| `java.time.LocalDate`     | `localDate`          | Uses `string` under the hood using (`DateTimeFormatter.ISO_LOCAL_DATE`)       |
| `java.time.LocalDateTime` | `localDateTime`      | Uses `string` under the hood using (`DateTimeFormatter.ISO_LOCAL_DATE_TIME`)  |


### `imap` - invariant/iso map

Each combinator (e.g.: `int`) supports has the `imap` combinator which allows you to define a isomorphism/invariant map. The original type is used to store the value. This is useful when you use value classes for your identifiers for example:

```scala
case class UserId(id: Int)

case class UserV1(userId: UserId)

object UserV1 {
  implicit val codec: Avro[UserV1] = record1("user", "User")(UserV1.apply)(
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId)
  )
}
```

### `pmap` - partial/prism map

Each combinator (e.g.: `int`) supports also has the `pmap` combinator which allows you to define a prism/partial map. The original type is used to store the value. While encoding we always have the function of `f: B => A`, while decoding we have the function of `f: A => Attempt[B]`.

The decoding might fail, therefore we return a `Attempt[A]`. If you would like to support for example string based enumerations you can do it yourself:

```scala
trait Enum[A] {
  val allValues: Set[A]
  def asString(value: A): String
}

object Enum {
  def apply[A](values: Set[A])(stringify: A => String): Enum[A] = new Enum[A] {
    override val allValues: Set[A] = values
    override def asString(value: A): String = stringify(value)
  }
}

object Color {
  case object Black extends Color("black")
  case object White extends Color("white")
  case object Orange extends Color("orange")

  val all: Set[Color] = Set(Black, White, Orange)

  implicit val enum: Enum[Color] = Enum(all)(_.repr)
}

def enum[A](implicit E: Enum[A]) =
    string.pmap(str => E.allValues.find(x => E.asString(x) == str).fold[Attempt[A]](Attempt.error(s"Value $str not found"))(Attempt.success))(E.asString)

```

#### Attempt

Attempt has two cases `Success` and `Error`. It supports several combinators

- `Attempt.fromEither` - Convert a `Either[L, R]` to `Attempt[R]`
- `Attempt.fromTry` - Convert a `Try[A]` to `Attempt[A]`
- `Attempt.fromOption` - Convert a `Option[A]` to `Attempt[A]`

### ADT support

TODO


### Generate schemas

TODO

### Compatibility checks

```scala
val v1 = AvroSchema[UserV1].generateSchema
val v2 = AvroSchema[UserV2].generateSchema

// equals AvroSchemaCompatibility.Full as v2 has default values
AvroSchemaCompatibility(writer = v1, reader = v2)

```

#### Default values

TODO

### Refined support

Add the module `formulation-refined`

```scala
import formulation._
import formulation.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined._
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric.Positive

case class PersonRefined(name: String Refined NonEmpty, age: Int Refined Positive)

object PersonRefined {
  implicit val codec: Avro[PersonRefined] = record2("user", "Person")(PersonRefined.apply)(
    "name" -> member(string.refine[NonEmpty], _.name),
    "age" -> member(int.refine[Positive], _.age)
  )
}
```

## Current performance

```
Benchmark                           Mode  Cnt        Score       Error  Units
DecodeBenchmark.decodeAvro4s       thrpt   20    88994.467 ±  5738.631  ops/s
DecodeBenchmark.decodeCirce        thrpt   20   939287.181 ± 19078.247  ops/s
DecodeBenchmark.decodeFormulation  thrpt   20   201500.182 ±  5415.471  ops/s
EncodeBenchmark.encodeAvro4s       thrpt   20   320479.584 ±  3460.697  ops/s
EncodeBenchmark.encodeCirce        thrpt   20  1036298.004 ± 18339.424  ops/s
EncodeBenchmark.encodeFormulation  thrpt   20   899141.737 ±  9341.533  ops/s
```

- Encode performance is twice as fast as avro4s, close to circe.
- Decode is faster then avro4s, but much slower then circe (will investigate)
