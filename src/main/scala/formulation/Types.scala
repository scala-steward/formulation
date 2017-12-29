package formulation

import cats._
import org.apache.avro.generic.GenericRecord

final case class Member[F[_], A, B](typeClass: F[A], getter: B => A, defaultValue: Option[A] = None) {
  def mapTypeClass[G[_]](f: F ~> G): Member[G, A, B] = copy(typeClass = f(typeClass))
}

sealed trait AvroData

object AvroData {
  final case class Integer(value: Int) extends AvroData
  final case class Str(value: String) extends AvroData
  final case class Record(value: GenericRecord) extends AvroData
  final case class Items(items: List[AvroData]) extends AvroData
  case object Null extends AvroData
}