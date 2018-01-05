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


## How does it look like ?

```scala
import formulation._

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

  // Support for ADT's, note that every instance of BookingProcess has `int.discriminator`, this is used for discrimination while decoding
  implicit val codec: Avro[BookingProcess] =
    (dateDeselected | notStarted | cancelled).as[BookingProcess]
}

case class UserV1(userId: UserId, username: String, email: String, password: String)

object UserV1 {
  implicit val codec: Avro[UserV1] = record4("user", "User")(UserV1.apply)(
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId),
    "username" -> member(string, _.username),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password)
  )
}

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
    "age" -> member(option(int), _.age, Some(None)),
    "countries" -> member(list(string), _.countries, defaultValue = Some(List("Holland"))),
    "bookingProcess" -> member(BookingProcess.codec, _.bookingProcess, defaultValue = Some(BookingProcess.Cancelled(2))),
    "money" -> member(bigDecimal(300, 300), _.money, defaultValue = Some(1000))
  )
}
```

### Supported primitives

`Int`, `String`, `Float`, `BigDecimal`, `Array[Byte]`, `Double`, `Long`, `Option[A]`, `List[A]`, `Set[A]`, `Vector[A]`, `Seq[A]`, `Map[String, A]`, `UUID`, `Instant`, `LocalDate` and `LocalDateTime`

### `imap` - invariant/iso map

TODO

### `pmap` - partial/prism map

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
