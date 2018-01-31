---
layout: docs
title: Performance
---

# Performance

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