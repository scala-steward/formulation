package formulation

import org.scalatest.{Matchers, WordSpec}

class CompatibilitySpec extends WordSpec with Matchers {

  implicit val generic: Avro[Generic[Int]] = record1("user", "User")(Generic.apply[Int])("bla" -> member(int, _.value))

  val v1 = AvroSchema[UserV1].generateSchema
  val v2 = AvroSchema[UserV2].generateSchema
  val v3 = AvroSchema[UserV3].generateSchema
  val genericSchema = AvroSchema[Generic[Int]].generateSchema

  "Compatiblity" should {
    "return Full for UserV1 and UserV2 - defaults given" in {
      AvroSchemaCompatibility(writer = v1, reader = v2) shouldBe AvroSchemaCompatibility.Full
    }

    "return Foward for UserV1 and UserV3 - no defaults" in {
      AvroSchemaCompatibility(writer = v1, reader = v3) shouldBe AvroSchemaCompatibility.Forward
    }

    "return Backward for UserV1 and UserV3 - no defaults" in {
      AvroSchemaCompatibility(writer = v3, reader = v1) shouldBe AvroSchemaCompatibility.Backward
    }

    "return NotCompatible for UserV1 and Generic" in {
      AvroSchemaCompatibility(writer = v3, reader = genericSchema) shouldBe AvroSchemaCompatibility.NotCompatible
    }

    "work while encoding as V1, we should get the right default values when decoding as V2" in {

      val bytes = encode(UserV1(UserId(1), "Mark", "mail@markdejong.org", "it-so-secret"))

      decode[UserV2](bytes, Some(v1)) shouldBe Attempt.success(
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
    "userId" -> member(int.imap(UserId.apply)(_.id), _.userId),
    "username" -> member(string, _.username),
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