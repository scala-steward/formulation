---
layout: docs
title: Akka serializer
---
 
# Akka serializer

To use the akka serializer you have to include the `formulation-akka-serializer` in your SBT.

You can use the akka serializer with akka remoting and akka persistence.

To not include the `Schema` on each message, we use the schema registry.

Here is a example:

```tut:silent
import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.avro.Schema
import _root_.scalacache.Entry
import _root_.scalacache.modes.try_._ // note: need this for running as `Try`
import _root_.scalacache.caffeine.CaffeineCache

import com.softwaremill.sttp.TryHttpURLConnectionBackend
import formulation._
import formulation.schemaregistry._
import formulation.schemaregistry.confluent.sttp.ConfluentSttpSchemaRegistryClient
import formulation.schemaregistry.scalacache.ScalacacheSchemaRegistryClient
import formulation.akkaserializer.FormulationAkkaSerializer

import cats.implicits._ // note: you need this for `MonadError[Try, Throwable]`
import java.time.Instant

sealed trait Event extends Serializable

case class Completed(at: Instant) extends Event
case class Failed(at: Instant) extends Event
case class Started(at: Instant) extends Event

val completedAvroCodec: Avro[Completed] = record1("event", "Completed")(Completed.apply)("at" -> member(instant, _.at))
val failedAvroCodec: Avro[Failed] = record1("event", "Failed")(Failed.apply)("at" -> member(instant, _.at))
val startedAvroCodec: Avro[Started] = record1("event", "Started")(Started.apply)("at" -> member(instant, _.at))

implicit val eventAvroCodec: Avro[Event] = (completedAvroCodec | failedAvroCodec | startedAvroCodec).as[Event]

val catsSttpBackend = TryHttpURLConnectionBackend()
val confluentSttpSchemaRegistryClient = ConfluentSttpSchemaRegistryClient("http://localhost:8081", catsSttpBackend)
val schemaCache = Caffeine.newBuilder().recordStats().maximumSize(10000L).build[String, Entry[Int]]
val idCache = Caffeine.newBuilder().recordStats().maximumSize(10000L).build[String, Entry[Schema]]
val cachedRegistryClient = ScalacacheSchemaRegistryClient(confluentSttpSchemaRegistryClient, CaffeineCache(schemaCache), CaffeineCache(idCache))
val schemaRegistry = SchemaRegistry(cachedRegistryClient)

class EventSerializer extends FormulationAkkaSerializer[Event](1337, SchemaRegistry(cachedRegistryClient))
```

Note that the akka serializer requires you to have a **synchronous** implementation of schema registry. 
In this example it works because we use `TryHttpURLConnectionBackend` and a synchronous cache implementation (caffeine).

The `FormulationAkkaSerializer` will not work with generic container types like for instance `Event[UserEvent]`. This
is because it uses `ClassTag`.

You can then use this akka-serializer to register it:

```
akka {

  actor {
    allow-java-serialization = off

    serializers {
      event-serializer = "formulation.example.EventSerializer"
    }
    serialization-bindings {
      "formulation.example.Event" = event-serializer
    }
  }
}
```