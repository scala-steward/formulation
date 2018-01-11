package formulation.schemaregistry.confluent.sttp

import cats.implicits._
import com.softwaremill.sttp._
import formulation.AvroSchemaCompatibility
import formulation.schemaregistry.SchemaRegistryClient
import jawn.Parser
import jawn.ast.JValue
import org.apache.avro.Schema

import scala.util.Try
import scala.util.control.NonFatal

final class ConfluentSttpSchemaRegistryClient[F[_]] private (baseUri: String)
                                                            (implicit B: SttpBackend[F, Nothing], M: cats.MonadError[F, Throwable]) extends SchemaRegistryClient[F] {

  private val schemaParser = new Schema.Parser()

  override def getSchemaById(id: Int): F[Option[Schema]] = {

    def parseResponse(resp: Response[String]): F[Option[Schema]] = for {
      body <- M.fromEither(resp.body.left.map(err => new Throwable(s"Status code was ${resp.code} with body '$err'")))
      result <- M.fromTry(Try(schemaParser.parse(Parser.parseUnsafe[JValue](body).get("schema").asString)))
    } yield Some(result)

    for {
      resp <- sttp
        .get(uri"$baseUri/schemas/ids/$id")
        .response(asString)
        .send[F]
      result <- if (resp.code != 200) M.pure(None) else parseResponse(resp)
    } yield result
  }

  override def getIdBySchema(schema: Schema): F[Option[Int]] = {
    def parseResponse(resp: Response[String]): F[Option[Int]] = for {
      body <- M.fromEither(resp.body.left.map(err => new Throwable(s"Status code was ${resp.code} with body '$err'")))
      id <- M.fromTry(Try(Parser.parseUnsafe[JValue](body).get("id").asInt))
    } yield Some(id)

    for {
      resp <- sttp
        .post(uri"$baseUri/subjects/${schema.getFullName}")
        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
        .body(schemaAsJson(schema))
        .send[F]
      result <- if (resp.code != 200) M.pure(None) else parseResponse(resp)
    } yield result
  }

  override def registerSchema(schema: Schema): F[Int] =
    for {
      resp <- sttp
        .post(uri"$baseUri/subjects/${schema.getFullName}/versions")
        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
        .body(schemaAsJson(schema))
        .send[F]
      body <- M.fromEither(resp.body.left.map(err => new Throwable(s"Status code was ${resp.code} with body '$err'")))
      result <- M.fromTry(Try(Parser.parseUnsafe[JValue](body).get("id").asInt))
    } yield result

  override def checkCompatibility(schema: Schema): F[Boolean] =
    for {
      resp <- sttp
        .post(uri"$baseUri/compatibility/subjects/${schema.getFullName}/versions/latest")
        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
        .body(schemaAsJson(schema))
        .send[F]
      body <- M.fromEither(resp.body.left.map(err => new Throwable(s"Status code was ${resp.code} with body '$err'")))
      compat <- M.fromTry(Try(Parser.parseUnsafe[JValue](body).get("is_compatible").asBoolean))
    } yield compat

  private def schemaAsJson(schema: Schema) =
    s"""{"schema": "${schema.toString().replaceAll("\"", "\\\\\"")}"}"""


  override def getCompatibilityLevel(subject: String): F[Option[AvroSchemaCompatibility]] = {
    def parseResponse(resp: Response[String]): F[Option[AvroSchemaCompatibility]] = for {
      body <- M.fromEither(resp.body.left.map(err => new Throwable(s"Status code was ${resp.code} with body '$err'")))
      compat <- getCompatLevel(body, "compatibility").recoverWith { case NonFatal(_) => getCompatLevel(body, "compatibilityLevel") }
      result <- parseCompatLevel(compat, subject)
    } yield Some(result)

    for {
      resp <- sttp.get(uri"$baseUri/config/$subject").send[F]
      result <- if (resp.code != 200) M.pure(None) else parseResponse(resp)
    } yield result
  }


  override def setCompatibilityLevel(subject: String, desired: AvroSchemaCompatibility): F[AvroSchemaCompatibility] = for {
    resp <- sttp
      .put(uri"$baseUri/config/$subject")
      .header("Content-Type", "application/json")
      .body(s"""{"compatibility":"${desired.repr}"}""")
      .send[F]

    body <- M.fromEither(resp.body.left.map(err => new Throwable(s"Status code was ${resp.code} with body '$err'")))
    compat <- getCompatLevel(body, "compatibility").recoverWith { case NonFatal(_) => getCompatLevel(body, "compatibilityLevel") }
    result <- parseCompatLevel(compat, subject)
  } yield result

  private def getCompatLevel(body: String, key: String): F[String] =
    M.fromTry(Try(Parser.parseUnsafe[JValue](body).get(key).asString))

  private def parseCompatLevel(compat: String, subject: String): F[AvroSchemaCompatibility] =
    AvroSchemaCompatibility.all.find(_.repr == compat) match {
      case Some(v) => M.pure(v)
      case None => M.raiseError(new Throwable(s"Unrecognized level: $compat for $subject"))
    }
}

object ConfluentSttpSchemaRegistryClient {
  def apply[F[_]](baseUri: String, backend: SttpBackend[F, Nothing])(implicit M: cats.MonadError[F, Throwable]): SchemaRegistryClient[F] =
    new ConfluentSttpSchemaRegistryClient[F](baseUri)(backend, M)
}
