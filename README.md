formulation
---


[![Build Status](https://api.travis-ci.org/vectos/formulation.svg)](https://travis-ci.org/vectos/formulation)
[![codecov.io](http://codecov.io/github/vectos/formulation/coverage.svg?branch=master)](http://codecov.io/github/vectos/formulation?branch=master)
[![latest version](https://index.scala-lang.org/vectos/formulation/formulation-core/latest.svg)](https://index.scala-lang.org/vectos/formulation)

_formulation_ is a EDSL (embedded domain specific language) for describing Avro data types.

Why use Avro?

- Space and network efficience thanks to a reduced payload.
- Schema evolution intelligence and compatibility enforcing by using a schema registry (which also forces you to centralise schema storage).

Why use Formulation?

- _Expressive_ - It supports the most primitive data types in Scala and allows you to `imap` or `pmap` them. Once you've defined a `Avro[A]` for a type, you can reuse these definitions to build up bigger types.
- _Define data types by hand_ - Avro4s derives schema's, encoders and decoders "magically". While this is nice, it can become unwieldy when you have a nested type graph. I believe it's better to explicitly map your data types, so the cognitive process of defining a schema is part of your job instead of a magic macro. This will become important when you want to enforce full compatibility of your schema's
- _Concise_ - The combinators `imap` and `pmap` makes it easy to introduce support for new data types, while this is verbose in Avro4s. Also the DSL provides a way of describing a schema in type-safe and terse fashion instead of typing JSON files.

### Minimal example

```scala
import formulation._

case class Person(name: String, age: Int)

object Person {
  implicit val codec: Avro[Person] = record2("user", "Person")(Person.apply)(
    "name" -> member(string, _.name),
    "age" -> member(int, _.age)
  )
}

//encode to a Array[Byte], then decode which yields a `Either[AvroDecodeFailure, Person]`
decode[Person](encode(Person("Mark", 1337))

//will print the schema as JSON
schema[Person].toString
```

### Dependencies

You need to add the bintray resolver

```
resolvers += Resolver.bintrayRepo("fristi", "maven")
```

Format module: `"net.vectos" %% "formulation-$module" % "$version"` where $module (module below) and $version is current version (see top). 

| Module                            | Dependencies                  | Remark                                                                        |
| ----------------------------------|-------------------------------|-------------------------------------------------------------------------------|
| core                              | cats, shapeless, avro         | Contains the DSL and basic encoding/decoding/schema support                   |
| refined                           | core, refined                 | Refinement types support                                                      |
| schema-registry                   | core                          | Interfaces for interacting with schema-registry                               |
| schema-registry-confluent-sttp    | schema-registry, sttp         | `SchemaRegistryClient` confluent schema-registry sttp implementation          |
 
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
| `java.util.UUID`          | `uuid`               | Uses `string` under the hood                                                  |
| `java.time.Instant`       | `instant`            | Uses `long` under the hood                                                    |
| `java.time.LocalDate`     | `localDate`          | Uses `string` under the hood using (`DateTimeFormatter.ISO_LOCAL_DATE`)       |
| `java.time.LocalDateTime` | `localDateTime`      | Uses `string` under the hood using (`DateTimeFormatter.ISO_LOCAL_DATE_TIME`)  |
| `java.time.ZonedDateTime` | `zonedDateTime `     | Uses `string` under the hood using (`DateTimeFormatter.ISO_ZONED_DATE_TIME`)  |




### Records up till max arity (22 fields)

To define records you use the combinator `recordN` (where N = 1 till 22). The record combinator requires you to specify a namespace and name. Never change the name and namespace if you want to maintain compatibility!

Also per field you need to provide a pair of a field name (also never change this if you want to maintain compatibility) and a description of the member

- Which type (use the combinators above)
- How to access it (getter)
- Default value (see below)

We could add aliases later.

### encode and decode

By using `import formulation._` you can use the `encode` and `decode` when there is a implicit `Avro[A]`

The type signature of `encode` looks like this:

```
def encode[A](value: A)(implicit R: AvroEncoder[A], S: AvroSchema[A]): Array[Byte]
```

When there is a `Avro[A]` implicitly available we can summon a `AvroEncoder[A]` and a `AvroSchema[A]` as well.

The type signature of `decode` looks like this:

```
def decode[A](bytes: Array[Byte], writerSchema: Option[Schema] = None, readerSchema: Option[Schema] = None)(implicit R: AvroDecoder[A], S: AvroSchema[A]): Either[AvroDecodeFailure, A]
```

You can provide a `writerSchema` in case you know the schema it's written with. If it's not supplied it will default to the `S: AvroSchema[A]`.



### `imap` - invariant/iso map

Each combinator (e.g.: `int`) supports has the `imap` combinator which allows you to define a isomorphism/invariant map. The original type is used to store the value.

The signature of `imap` is:

```
def imap[B](f: A => B)(g: B => A): Avro[B]
```

While encoding we always have the function of `g: B => A`, while decoding we use `f: A => B` which maps the primitive type to a value class for example:

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

Each combinator (e.g.: `int`) supports also has the `pmap` combinator which allows you to define a prism/partial map. The original type is used to store the value.

The signature of `pmap` is:

```
def pmap[B](f: A => Attempt[B])(g: B => A): Avro[B]
```

While encoding we always have the function of `g: B => A`, while decoding we have the function of `f: A => Attempt[B]`.

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
    string.pmap(str => Attempt.fromOption(E.allValues.find(x => E.asString(x) == str), s"$str not found in the valid options list"))(E.asString)

```

#### Attempt

Attempt has two cases `Success`, `Exception` and `Error`. It has several combinators in it's companion object:

- `Attempt.fromEither` - Convert a `Either[L, R]` to `Attempt[R]`
- `Attempt.fromTry` - Convert a `Try[A]` to `Attempt[A]`
- `Attempt.fromOption` - Convert a `Option[A]` to `Attempt[A]`

### Sum types

Because we have the `or` combinator and Avro supports union we can also support sum types (a coproduct/sum type is isomorphic to nested `Either`):

```scala
sealed trait BookingProcess { val disc: Int }

object BookingProcess {
  final case class DateSelected(disc: Int, datetime: LocalDateTime) extends BookingProcess
  final case class NotStarted(disc: Int) extends BookingProcess
  final case class Cancelled(disc: Int) extends BookingProcess

  private val dateDeselected: Avro[BookingProcess.DateSelected] =
    record2("formulation", "DateSelected")(BookingProcess.DateSelected.apply)(
      "disc" -> member(int.discriminator(1), _.disc),
      "datetime" -> member(localDateTime, _.datetime)
    )

  private val notStarted: Avro[BookingProcess.NotStarted] =
    record1("formulation", "NotStarted")(BookingProcess.NotStarted.apply)(
      "disc" -> member(int.discriminator(0), _.disc)
    )

  private val cancelled: Avro[BookingProcess.Cancelled] =
    record1("formulation", "Cancelled")(BookingProcess.Cancelled.apply)(
      "disc" -> member(int.discriminator(2), _.disc)
    )

  implicit val codec: Avro[BookingProcess] =
    (dateDeselected | notStarted | cancelled).as[BookingProcess]
}
```

Note that we use `|` to denote a shapeless `Coproduct`, once the sum type has a complete definition you can cast it to the super type of the coproduct/sum type. In this case it's `BookingProcess` with the combinator `as`.

### Generate schemas

When you have a implicit `Avro[A]` available you can use `schema[A]` to get a `org.apache.avro.Schema`

```scala

case class UserV1(userId: UserId, username: String, email: String, password: String)

object UserV1 {
  implicit val codec: Avro[UserV1] = record4("user", "User")(UserV1.apply)(
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId),
    "username" -> member(string, _.username),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password)
  )
}

val v1: Schema = schema[UserV1]

// will print the JSON schema
v1.toString()
```

#### Default values

Above we defined `UserV1`, what if we have `UserV2` which has extra fields and we want to be full compatible? We need to define default values for the new fields `age`, `countries`, `bookingProcess` and `money`. Note `defaultValue` in the `member` method.

```scala
case class UserV2(
                   userId: UserId,
                   username: String,
                   email: String,
                   password: String,
                   age: Option[Int],
                   countries: List[String],
                   bookingProcess: BookingProcess,
                   money: BigDecimal
                 )

object UserV2 {
  implicit val codec: Avro[UserV2] = record8("user", "User")(UserV2.apply)(
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId),
    "username" -> member(string, _.username),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password),
    "age" -> member(option(int), _.age, defaultValue = Some(None)),
    "countries" -> member(list(string), _.countries, defaultValue = Some(List("Holland"))),
    "bookingProcess" -> member(BookingProcess.codec, _.bookingProcess, defaultValue = Some(BookingProcess.Cancelled(2))),
    "money" -> member(bigDecimal(300, 300), _.money, defaultValue = Some(1000))
  )
}
```

These values are also outputted in the JSON of the schema.

### Compatibility checks

```scala
val v1: Schema = schema[UserV1]
val v2: Schema = schema[UserV2]

// equals AvroSchemaCompatibility.Full as v2 has default values
AvroSchemaCompatibility(writer = v1, reader = v2)

```


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

## Performance

### Encode (single method-invocation)

```
SingleEncodeBenchmark.benchAvro4s              thrpt   20   339782.352 ± 28237.119  ops/s
SingleEncodeBenchmark.benchCirce               thrpt   20   897614.438 ± 23166.022  ops/s
SingleEncodeBenchmark.benchFormulation         thrpt   20   823455.552 ± 17417.526  ops/s
SingleEncodeBenchmark.benchFormulationKleisli  thrpt   20  2345298.160 ± 67476.440  ops/s
```

- formulation processes ~823.000 event/s and ~2.300.000 event/s with the `Kleisli` variant (as `AvroSchema` and `AvroEncoder` are already materialized)
- circe processes ~900.000 event/s
- avro4s processes ~340.000 event/s


### Decode (single method-invocation)

```
Benchmark                                       Mode  Cnt        Score       Error  Units
SingleDecodeBenchmark.benchAvro4s              thrpt   20    95355.197 ±  5487.262  ops/s
SingleDecodeBenchmark.benchCirce               thrpt   20   899267.015 ± 21630.452  ops/s
SingleDecodeBenchmark.benchFormulation         thrpt   20   184089.676 ±  5330.773  ops/s
SingleDecodeBenchmark.benchFormulationKleisli  thrpt   20  1008149.642 ± 21308.270  ops/s
```

- formulation processes ~185.000 event/s and ~1.000.000 event/s with the `Kleisli` variant (as `AvroSchema` and `AvroDecoder` are already materialized)
- circe processes ~900.000 event/s
- avro4s processes ~95.000 event/s

### Decode (akka-streams)

```
Benchmark                               (size)   Mode  Cnt   Score   Error  Units
StreamDecodeBenchmark.benchAvro4s        10000  thrpt   20   8.326 ± 0.569  ops/s
StreamDecodeBenchmark.benchAvro4s       100000  thrpt   20   0.815 ± 0.058  ops/s
StreamDecodeBenchmark.benchCirce         10000  thrpt   20  82.767 ± 3.007  ops/s
StreamDecodeBenchmark.benchCirce        100000  thrpt   20   8.244 ± 0.189  ops/s
StreamDecodeBenchmark.benchFormulation   10000  thrpt   20  85.927 ± 2.065  ops/s
StreamDecodeBenchmark.benchFormulation  100000  thrpt   20   8.815 ± 0.253  ops/s
```

- formulation processes ~880.000 event/s
- circe processes ~820.000 event/s
- avro4s processes ~83.000 event/s

### Encode (akka-streams)

```
Benchmark                               (size)   Mode  Cnt      Score     Error  Units
StreamEncodeBenchmark.benchAvro4s        10000  thrpt   20     30.666 ±   0.501  ops/s
StreamEncodeBenchmark.benchAvro4s       100000  thrpt   20      2.924 ±   0.102  ops/s
StreamEncodeBenchmark.benchCirce         10000  thrpt   20     92.609 ±   3.042  ops/s
StreamEncodeBenchmark.benchCirce        100000  thrpt   20      8.436 ±   0.372  ops/s
StreamEncodeBenchmark.benchFormulation   10000  thrpt   20    147.233 ±   8.337  ops/s
StreamEncodeBenchmark.benchFormulation  100000  thrpt   20     15.104 ±   0.562  ops/s
```

- formulation processes ~1.500.000 event/s
- circe processes ~840.000 event/s
- avro4s processes ~300.000 event/s
