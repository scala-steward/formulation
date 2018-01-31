---
layout: docs
title: The basics
---

```tut:invisible
import formulation._
```

# The basics

## Supported primitives

Every **combinator** listed below will return a `Avro[A]` (where `A` is the **type**).


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

## Records up till max arity (22 fields)

To define records you use the combinator `recordN` (where N = 1 till 22). The record combinator requires you to specify a namespace and name. Never change the name and namespace if you want to maintain compatibility!

Also per field you need to provide a pair of a field name (also never change this if you want to maintain compatibility) and a description of the member

- Which type (use the combinators above)
- How to access it (getter)
- Default value (see below)

We could add aliases later.

## encode and decode

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


## Generate schemas

When you have a implicit `Avro[A]` available you can use `schema[A]` to get a `org.apache.avro.Schema`

```tut:silent
case class UserV1(userId: Int, username: String, email: String, password: String)

implicit val codecV1: Avro[UserV1] = record4("user", "User")(UserV1.apply)(
  "userId" -> member(int, _.userId),
  "username" -> member(string, _.username),
  "email" -> member(string, _.email),
  "password" -> member(string, _.password)
)
```

Print it

```tut
schema[UserV1].toString(true)
```

### Default values

Above we defined `UserV1`, what if we have `UserV2` which has extra fields and we want to be full compatible? We need to define default values for the new fields `age`, `countries`, `bookingProcess` and `money`. Note `defaultValue` in the `member` method.

```tut:silent
case class UserV2(
                   userId: Int,
                   username: String,
                   email: String,
                   password: String,
                   age: Option[Int],
                   countries: List[String],
                   money: BigDecimal
                 )

implicit val codecV2: Avro[UserV2] = record7("user", "User")(UserV2.apply)(
    "userId" -> member(int, _.userId),
    "username" -> member(string, _.username),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password),
    "age" -> member(option(int), _.age, defaultValue = Some(None)),
    "countries" -> member(list(string), _.countries, defaultValue = Some(List("Holland"))),
    "money" -> member(bigDecimal(300, 300), _.money, defaultValue = Some(1000))
)
```

These values are also outputted in the JSON of the schema.

## Compatibility checks

`AvroSchemaCompatibility.Full` as `v2` has default values

```tut
AvroSchemaCompatibility(schema[UserV1], schema[UserV2])
```