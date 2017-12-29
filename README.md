formulation
---

_formulation_ is a EDSL (embedded domain specific language) for describing Avro records. Why would you like that? Well if you have a schema-registry which forbids to have incompatible schemas, you want to be explicit as possible to have the cognitive process being triggered to fix any incompatibility. While avro4s is a nice library it is too magical (through derivation) and defining custom types is a bit verbose.

## How does it look like ?

```scala
import formulation._

sealed abstract class Color(val repr: String)

object Color {
  case object Black extends Color("black")
  case object White extends Color("white")
  case object Orange extends Color("orange")

  val all: Set[Color] = Set(Black, White, Orange)

  implicit val enum: Enum[Color] = Enum(all)(_.repr)
}

trait Enum[A] {
  val allValues: Set[A]
  def asString(value: A): String
}

object Enum {
  def apply[A](values: Set[A])(stringify: A => String): Enum[A] = new Enum[A] {
    override val allValues: Set[A] = values
    override def asString(value: A): String = stringify(value)
  }
}

case class Address(street: String, houseNumber: Int)
case class Person(name: String, favoriteColor: Color, address: Address)

object Main extends App {

  def enum[A](implicit E: Enum[A]) =
    string.pmap(str => E.allValues.find(x => E.asString(x) == str).fold[Either[Throwable, A]](Left(new Throwable(s"Value $str not found")))(Right.apply))(E.asString)

  implicit val address: Avro[Address] = record3(namespace = "forma", name = "Address")(Address.apply)(
    "street" -> Member(string, _.street),
    "houseNumber" -> Member(int, _.houseNumber, defaultValue = 0)
  )
  implicit val person: Avro[Person] = record3(namespace = "forma", name = "Person")(Person.apply)(
    "name" -> Member(string, _.name),
    "favoriteColor" -> Member(enum[Color], _.favoriteColor),
    "address" -> Member(address, _.address)
  )

  println(decode[Person](encode(Person("Mark", Color.Orange, Address("Scalastreet", 4))))
}
```


