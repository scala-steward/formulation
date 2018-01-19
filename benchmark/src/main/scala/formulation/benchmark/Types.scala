package formulation.benchmark

import formulation._

trait BenchSuite {
  def benchAvro4s(): Unit
  def benchFormulation(): Unit
  def benchCirce(): Unit
}

case class UserId(id: Int) extends AnyVal

case class UserV1(userId: Int, username: String, email: String, password: String)

object UserV1 {
  implicit val codec: Avro[UserV1] = record4("user", "User")(UserV1.apply)(
    "userId" -> member(int, _.userId),
    "username" -> member(string, _.username),
    "email" -> member(string, _.email),
    "password" -> member(string, _.password)
  )
}