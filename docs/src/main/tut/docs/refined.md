---
layout: docs
title: Refined support
---

# Refined support

Add the module `formulation-refined`

```tut:silent
import formulation._
import formulation.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive

case class PersonRefined(name: String Refined NonEmpty, age: Int Refined Positive)

implicit val codec: Avro[PersonRefined] = record2("user", "Person")(PersonRefined.apply)(
    "name" -> member(string.refine[NonEmpty], _.name),
    "age" -> member(int.refine[Positive], _.age)
)
```