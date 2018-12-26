---
layout: home
title:  "Home"
section: "home"
technologies:
 - first: ["Expressive", "It supports the most primitive data types in Scala and allows you to `imap` or `pmap` them. Once you've defined a `Avro[A]` for a type, you can reuse these definitions to build up bigger types."]
 - second: ["Define data types by hand", "Avro4s derives schema's, encoders and decoders magically. While this is nice, it can become unwieldy when you have a nested type graph. I believe it's better to explicitly map your data types, so the cognitive process of defining a schema is part of your job instead of a magic macro. This will become important when you want to enforce full compatibility of your schema's"]
 - third: ["Concise", "The combinators `imap` and `pmap` makes it easy to introduce support for new data types, while this is verbose in Avro4s. Also the DSL provides a way of describing a schema in type-safe and terse fashion instead of typing JSON files."]
---

[![Build Status](https://api.travis-ci.org/vectos/formulation.svg)](https://travis-ci.org/vectos/formulation)
[![codecov.io](http://codecov.io/github/vectos/formulation/coverage.svg?branch=master)](http://codecov.io/github/vectos/formulation?branch=master)
[![latest version](https://index.scala-lang.org/vectos/formulation/formulation-core/latest.svg)](https://index.scala-lang.org/vectos/formulation)

## Minimal example and get started!

```tut:silent
import formulation._

case class Person(name: String, age: Int)

implicit val codec: Avro[Person] = record2("user", "Person")(Person.apply)(
    "name" -> member(string, _.name),
    "age" -> member(int, _.age)
)
```

Head over to "[Getting started](docs/index.html)" to get started



