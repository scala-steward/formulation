package formulation

import org.apache.avro.Schema
import org.scalatest.{Matchers, WordSpec}

class CompatibilitySpec extends WordSpec with Matchers {

  implicit val generic: Avro[Generic[Int]] = record1("user", "User")(Generic.apply[Int])("bla" -> member(int, _.value))

  val v1: Schema = schema[UserV1]
  val v2: Schema = schema[UserV2]
  val v3: Schema = schema[UserV3]
  val genericSchema: Schema = schema[Generic[Int]]

  "Compatiblity" should {
    "return Full for UserV1 and UserV2 - defaults given" in {
      AvroSchemaCompatibility(schema1 = v1, schema2 = v2) shouldBe AvroSchemaCompatibility.Full
    }

    "return Foward for UserV1 and UserV3 - no defaults" in {
      AvroSchemaCompatibility(schema1 = v1, schema2 = v3) shouldBe AvroSchemaCompatibility.Forward
    }

    "return Backward for UserV1 and UserV3 - no defaults" in {
      AvroSchemaCompatibility(schema1 = v3, schema2 = v1) shouldBe AvroSchemaCompatibility.Backward
    }

    "return NotCompatible for UserV1 and Generic" in {
      AvroSchemaCompatibility(schema1 = v3, schema2 = genericSchema) shouldBe AvroSchemaCompatibility.None
    }

    "work while encoding as V1, we should get the right default values when decoding as V2" in {

      val bytes = encode(UserV1(UserId(1), "Mark", "mail@markdejong.org", "it-so-secret"))

      decode[UserV2](bytes, Some(v1)) shouldBe Right(
        UserV2(UserId(1), "Mark", "mail@markdejong.org", "it-so-secret", None, List("Holland"), BookingProcess.Cancelled(2), 1000))

    }
  }

}

case class UserV1(userId: UserId, username: String, email: String, password: String)

object UserV1 {
  implicit val codec: Avro[UserV1] = record4("user", "User")(UserV1.apply)(
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId),
    "username" -> member(string, _.username),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password)
  )
}

case class UserV2(
                   userId: UserId,
                   username: String,
                   email: String,
                   password: String,
                   age: Option[Int],
                   countries: List[String],
                   bookingProcess: BookingProcess,
                   money: BigDecimal
                 )

object UserV2 {
  implicit val codec: Avro[UserV2] = record8("user", "User")(UserV2.apply)(
    //compatibility spec: we added aliases
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId, aliases = Seq("id")),
    //compatibility spec: we added documentation, due to canonical parsing this should not break compatibility
    "username" -> member(string, _.username, documentation = Some("The username of the user")),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password),
    "age" -> member(option(int), _.age, Some(None)),
    "countries" -> member(list(string), _.countries, Some(List("Holland"))),
    "bookingProcess" -> member(BookingProcess.codec, _.bookingProcess, Some(BookingProcess.Cancelled(2))),
    "money" -> member(bigDecimal(300, 300), _.money, Some(1000))
  )
}

case class UserV3(
                   userId: UserId, username: String, email: String, password: String, age: Int
                 )

object UserV3 {
  implicit val codec: Avro[UserV3] = record5("user", "User")(UserV3.apply)(
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId),
    "username" -> member(string, _.username),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password),
    "age" -> member(int, _.age)
  )
}

case class UserV4(
                   userId: UserId,
                   username: String,
                   email: String,
                   password: String,
                   age: Option[Int],
                   countries: List[String],
                   bookingProcess: BookingProcess
                 )

object UserV4 {
  implicit val codec: Avro[UserV4] = record7("user", "User")(UserV4.apply)(
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId),
    "username" -> member(string, _.username),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password),
    "age" -> member(option(int), _.age, Some(None)),
    "countries" -> member(list(string), _.countries, Some(List("Holland"))),
    "bookingProcess" -> member(BookingProcess.codec, _.bookingProcess, Some(BookingProcess.Cancelled(2)))
  )
}