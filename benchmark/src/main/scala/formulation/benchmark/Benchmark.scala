package formulation.benchmark

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.concurrent.TimeUnit

import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream}
import formulation.{Avro, int, member, record4, string}

import io.circe.generic.auto._
import io.circe.syntax._

import org.openjdk.jmh.annotations._


@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class EncodeBenchmark {

  def user = UserV1(1, "Mark", "mark@vectos.net", "so-secret")

  @Benchmark
  def encodeAvro4s(): Unit = {
    val baos = new ByteArrayOutputStream()
    val output = AvroOutputStream.binary[UserV1](baos)
    output.write(user)
    output.close()
    val bytes = baos.toByteArray
  }
  @Benchmark
  def encodeFormulation(): Unit = formulation.encode(user)
  @Benchmark
  def encodeCirce(): Unit = {
    user.asJson.noSpaces
  }
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DecodeBenchmark {

  val user: UserV1 = UserV1(1, "Mark", "mark@vectos.net", "so-secret")
  val json: String = user.asJson.noSpaces
  val avroBytes: Array[Byte] = formulation.encode(user)

  @Benchmark
  def decodeAvro4s(): Unit = {
    val in = new ByteArrayInputStream(avroBytes)
    val input = AvroInputStream.binary[UserV1](in)
    val result = input.iterator.toSeq
  }

  @Benchmark
  def decodeFormulation(): Unit = formulation.decode[UserV1](avroBytes)

  @Benchmark
  def decodeCirce(): Unit = {
    io.circe.parser.decode[UserV1](json)
  }
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
