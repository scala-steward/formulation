---
layout: docs
title: Akka streams
---
 
# Akka streams

To use the akka serializer you have to include the `formulation-akka-streams` in your SBT.

### Kleisli variants of encode and decode

Let's start with some imports again.

```tut:silent
import formulation._
```

Note that we have beside `encode` and `decode`, we also have
`kleisliEncode` and `kleisliDecode`. Actually `encode` and `decode` are simplified variants of
the Kleisli variants.

Why do we have the Kleisli variants?

- We compose the Kleisli variants later on in `SchemaRegistry`
    - For encoding we first use the `formulation.kleisliEncode` and then we put this into a Confluent envelope
    - For decoding we first read the envelope (this has the schema identifier) and then use `formulation.kleisliDecode`
- The Kleisli variants use a `AvroDecodeContext` and `AvroEncodeContext`. These contexts are used to carry around and reuse the `BinaryEncoder` and `BinaryDecoder`. This is useful in streaming or encoding/decoding to gain performance
- The materialization of type classes like `AvroEncoder`, `AvroDecoder` and `AvroSchema` only will take place _once_, while with `encode` and `decode` it will trigger each time.

### Use the Kleisli!

```tut:silent
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

import cats.implicits._

implicit val system: ActorSystem = ActorSystem()
implicit val materializer: Materializer = ActorMaterializer()

case class Person(name: String, age: Int)

implicit val codecPerson: Avro[Person] = record2("entity", "Person")(Person.apply)("name" -> member(string, _.name), "age" -> member(int, _.age)) 

val encoder = akkastreams.kleisliEncode(kleisliEncode[Try, Person].mapF(Future.fromTry))
val decoder = akkastreams.kleisliDecode(kleisliDecode[Try, Person]().mapF(Future.fromTry))

def stream = Source.single(Person("Mark", 1337)).via(encoder).via(decoder).runWith(Sink.head)
```

Tada!

```tut
Await.result(stream << system.terminate(), 10.seconds)
```

Note that if you use top level unions, you'll need to use a `SchemaRegistry` to discriminate the messages.




