---
layout: docs
title: Pmap, imap and sum types
---

```tut:invisible
import formulation._
import java.time.LocalDateTime
```

# Pmap, imap and sum types

## `imap` - invariant/iso map

Each combinator (e.g.: `int`) supports has the `imap` combinator which allows you to define a isomorphism/invariant map. The original type is used to store the value.

The signature of `imap` is:

```
def imap[B](f: A => B)(g: B => A): Avro[B]
```

While encoding we always have the function of `g: B => A`, while decoding we use `f: A => B` which maps the primitive type to a value class for example:

```tut:silent
case class UserId(id: Int)

case class UserV1(userId: UserId)

implicit val codecUserV1: Avro[UserV1] = record1("user", "User")(UserV1.apply)(
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId)
)
```

## `pmap` - partial/prism map

Each combinator (e.g.: `int`) supports also has the `pmap` combinator which allows you to define a prism/partial map. The original type is used to store the value.

The signature of `pmap` is:

```
def pmap[B](f: A => Attempt[B])(g: B => A): Avro[B]
```

While encoding we always have the function of `g: B => A`, while decoding we have the function of `f: A => Attempt[B]`.

The decoding might fail, therefore we return a `Attempt[A]`. If you would like to support for example string based enumerations you can do it yourself:

```tut:silent
trait Enum[A] {
  val allValues: Set[A]
  def asString(value: A): String
}

sealed abstract class Color(val repr: String)

case object Black extends Color("black")
case object White extends Color("white")
case object Orange extends Color("orange")

val all: Set[Color] = Set(Black, White, Orange)

implicit val enum: Enum[Color] = new Enum[Color] {
    val allValues = all
    def asString(value: Color): String = value.repr
}

def enum[A](implicit E: Enum[A]): Avro[A] =
    string.pmap(str => Attempt.fromOption(E.allValues.find(x => E.asString(x) == str), s"$str not found"))(E.asString)

```

### Attempt

Attempt has two cases `Success`, `Exception` and `Error`. It has several combinators in it's companion object:

- `Attempt.fromEither` - Convert a `Either[L, R]` to `Attempt[R]`
- `Attempt.fromTry` - Convert a `Try[A]` to `Attempt[A]`
- `Attempt.fromOption` - Convert a `Option[A]` to `Attempt[A]`

## Sum types

Because we have the `or` combinator and Avro supports union we can also support sum types (a coproduct/sum type is isomorphic to nested `Either`):

```tut:silent
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
