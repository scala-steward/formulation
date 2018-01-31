---
layout: docs
title: Getting Started
---

# Getting started

Let's start with the dependencies

## Dependencies


You need to add the bintray resolver

```
resolvers += Resolver.bintrayRepo("fristi", "maven")
```

Format module: `"net.vectos" %% "formulation-$module" % "$version"` where $module (module below) and $version is current version. 

| Module                                                                                                                      | Dependencies                  | Docs                                                                                    |
| ----------------------------------------------------------------------------------------------------------------------------|-------------------------------|-----------------------------------------------------------------------------------------|
| ![latest version](https://index.scala-lang.org/vectos/formulation/formulation-core/latest.svg)                              | cats, shapeless, avro         | [Basics](basics.html) and [Imap, pmap and sum types](richer-data-types.html)  |
| ![latest version](https://index.scala-lang.org/vectos/formulation/formulation-refined/latest.svg)                           | core, refined                 | [Refinement types](refined.html)                                                   |
| ![latest version](https://index.scala-lang.org/vectos/formulation/formulation-schema-registry/latest.svg)                   | core                          | [Schema registry](schema-registry.html)                                            |
| ![latest version](https://index.scala-lang.org/vectos/formulation/formulation-schema-registry-confluent-sttp/latest.svg)    | schema-registry, sttp         | [Schema registry](schema-registry.html)                                            |
| ![latest version](https://index.scala-lang.org/vectos/formulation/formulation-schema-registry-scalacache/latest.svg)        | schema-registry, scalacache   | [Schema registry](schema-registry.html)                                            |
| ![latest version](https://index.scala-lang.org/vectos/formulation/formulation-akka-serializer/latest.svg)                   | schema-registry, akka-actor   | [Akka serializer](akka-serializer.html)                                            |
| ![latest version](https://index.scala-lang.org/vectos/formulation/formulation-akka-streams/latest.svg)                      | schema-registry, akka-stream  | [Akka streams](akka-streams.html)                                                  |


## Minimal example

Let's define a data type and a `Avro` definition for it implicitly.

```tut:silent
import formulation._

case class Person(name: String, age: Int)

implicit val codec: Avro[Person] = record2("user", "Person")(Person.apply)(
    "name" -> member(string, _.name),
    "age" -> member(int, _.age)
)
```

Now let's use it to encode and decode (encode to a `Array[Byte]`, then decode which yields a `Either[AvroDecodeFailure, Person]`)

```tut
decode[Person](encode(Person("Mark", 1337)))
```

We can also print the schema:

```tut
schema[Person].toString(true)
```