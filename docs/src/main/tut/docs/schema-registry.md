---
layout: docs
title: Schema registry
---

# Schema registry

### What is schema registry?

Schema registry is a centralised database for storing `Schema`'s. The `Schema`'s are stored per subject and per subject it's
possible to enforce compatibility. Each `Schema` also has a identifier in the schema registry. 
This identifier is used to put next to a Avro payload, such that it's not required to send the `Schema` with each message. 

### Wire format
In formulation we use the standardised wire format of Confluent, which can be found [here](https://docs.confluent.io/current/schema-registry/docs/serializer-formatter.html#wire-format)   

### Use cases
Using this wire format can be useful in many applications where full, backward or forward compatibility is required. Examples
of use cases are for example

- Produce/consume messages to/from Kafka
- Store/receive payloads to/from Redis
- Store/receive events to/from a event journal (akka-persistence)

### Using it

The "reference" implementation of schema registry is developed by Confluent (maintainers and new development of Kafka).
It is a REST API for this which is based on Kafka/Java. For this API there is a docker image available [here](https://hub.docker.com/r/confluentinc/cp-schema-registry/).
It's usually nice to run something like the [fast-data-dev](https://hub.docker.com/r/landoop/fast-data-dev/) stack while development, to inspect messages put on Kafka and inspect the
schema registry.

The API is pretty easy and could also be implemented in terms of a SQL database, K/V store or maybe even a CRDT based one.

## How to use it in formulation

Let's start with some imports

```tut:silent
import formulation._
import formulation.schemaregistry._
```

## Data types used in formulation

This will bring in `SchemaRegistry`. The `SchemaRegistry` is defined as a tagless final. Let's have a look at the constructor of `SchemaRegistry`. Note that the effect type is not set yet:

```scala
def apply[F[_]](client: SchemaRegistryClient[F])(implicit F: MonadError[F, Throwable]): SchemaRegistry[F]
```

The effect type is constrained to be a `MonadError[F, Throwable]`. This means the effect type needs to be able to raise errors
as `Throwable`. Examples of effect types which are supported are:

- `scala.util.Try` - for synchronous computations
- `scala.concurrent.Future` - for asynchronous computations
- `cats.effect.IO` - for synchronous/asynchronous computations

Note that you need to have type class instances in scope to make this work. Typically this means `import cats.implicits._` (as `MonadError` comes from **cats**)

The most notable methods on `SchemaRegistry` are:

- `def encode[A: AvroSchema : AvroEncoder](value: A): F[Array[Byte]]` - Encodes a entity using the wire format
- `def decode[A: AvroSchema : AvroDecoder](bytes: Array[Byte]): F[Either[AvroDecodeFailure, A]]` - Decodes a entity using the wire format
- `def verifyCompatibility[A](avro: Avro[A], desired: AvroSchemaCompatibility = AvroSchemaCompatibility.Full): F[List[SchemaRegistryCompatibilityResult]]` - Checks the compatibility
- `def registerSchemas[A](avro: Avro[A]): F[List[SchemaRegistryRegisterResult]]` - Registers the schema's

The `SchemaRegistryClient` trait is also defined in tagless final manner:

```scala
trait SchemaRegistryClient[F[_]] {
  def getSchemaById(id: Int): F[Option[Schema]]
  def getIdBySchema(schema: Schema): F[Option[Int]]
  def registerSchema(schema: Schema): F[Int]
  def checkCompatibility(schema: Schema): F[Boolean]
  def getCompatibilityLevel(subject: String): F[Option[AvroSchemaCompatibility]]
  def setCompatibilityLevel(subject: String, desired: AvroSchemaCompatibility): F[AvroSchemaCompatibility]
}
```

The first two methods are the most frequently used methods. 
- `getSchemaById` - is used while decoding
- `getIdBySchema` - is used while encoding

For that reason we suggest to cache these calls (by using Scalacache, scroll down to see how we support that)

## Top level union types

When you define a top level union (sum type) and you either register this or you check compatibility, `SchemaRegistry` will get all the schema's from the **UNION** schema and for each
schema in the **UNION** it will register or verify compatibility individually. So for example this algebraic data type:

```tut:silent
import java.time.Instant

sealed trait Event

case class Completed(at: Instant) extends Event
case class Failed(at: Instant) extends Event
case class Started(at: Instant) extends Event

val completedAvroCodec: Avro[Completed] = record1("event", "Completed")(Completed.apply)("at" -> member(instant, _.at))
val failedAvroCodec: Avro[Failed] = record1("event", "Failed")(Failed.apply)("at" -> member(instant, _.at))
val startedAvroCodec: Avro[Started] = record1("event", "Started")(Started.apply)("at" -> member(instant, _.at))

implicit val eventAvroCodec: Avro[Event] = (completedAvroCodec | failedAvroCodec | startedAvroCodec).as[Event]
```

Will cause three registration and verify compatibility calls with the  `SchemaRegistryClient`. The subjects used are `event.Completed`, `event.Failed` and `event.Started`. 
We did that because sometimes you want to add or remove a case from an algebraic data type without breaking the compatibility. 
In standard Avro, if you would add or remove a union member, it will break compatibility.

## Sttp support

To interact with the Confluent schema registry API we've implemented a `ConfluentSttpSchemaRegistryClient` which you can get by using the `formulation-schema-registry-confluent-sttp` module.

Example of using it:

```tut:silent
import com.softwaremill.sttp.TryHttpURLConnectionBackend
import formulation.schemaregistry.confluent.sttp.ConfluentSttpSchemaRegistryClient
import cats.implicits._ // note: you need this for `MonadError[Try, Throwable]`

val catsSttpBackend = TryHttpURLConnectionBackend()
val confluentSttpSchemaRegistryClient = ConfluentSttpSchemaRegistryClient("http://localhost:8081", catsSttpBackend)
```

Note that you can use different sttp backends as described in the documentation of [sttp](http://sttp.readthedocs.io/en/latest/backends/summary.html)  

## Scalacache support

As said above, we would like to cache the calls `getSchemaById` and `getIdBySchema`. For that reason we can use [scalacache](https://github.com/cb372/scalacache).
You can get by using the `formulation-schema-registry-scalacache` module.

Example of using it with Caffeine:

```tut:silent
import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.avro.Schema
import _root_.scalacache.Entry
import _root_.scalacache.modes.try_._ // note: need this for running as `Try`
import _root_.scalacache.caffeine.CaffeineCache
import formulation.schemaregistry.scalacache.ScalacacheSchemaRegistryClient

val schemaCache = Caffeine.newBuilder().recordStats().maximumSize(10000L).build[String, Entry[Int]]
val idCache = Caffeine.newBuilder().recordStats().maximumSize(10000L).build[String, Entry[Schema]]
val cachedRegistryClient = ScalacacheSchemaRegistryClient(confluentSttpSchemaRegistryClient, CaffeineCache(schemaCache), CaffeineCache(idCache))
```

Depending on how you want to cache the schema's you could either go for caffeine (cached per machine) or a distributed cache like Redis.

### Using the schema registry

Now we've set up the clients we can use it:

```tut:silent
val schemaRegistry = SchemaRegistry(cachedRegistryClient)
```

Let's register

```tut
schemaRegistry.registerSchemas(eventAvroCodec)
```

Let's encode and decode

```tut
for {
    bytes <- schemaRegistry.encode(Completed(Instant.now) : Event)
    result <- schemaRegistry.decode[Event](bytes)
} yield result
```

