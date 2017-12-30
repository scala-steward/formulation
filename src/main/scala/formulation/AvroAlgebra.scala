package formulation

trait AvroAlgebra[F[_]] extends AvroAlgebraRecordN[F] {
  val int: F[Int]
  val string: F[String]
//  val bool: F[Boolean]
//  val float: F[Float]
//  val bigDecimal: F[BigDecimal]
//  val byteArray: F[Array[Byte]]
//  val double: F[Double]
//  val long: F[Long]

  def option[A](from: F[A]): F[Option[A]]
  def list[A](of: F[A]): F[List[A]]
//  def set[A](of: F[A]): F[Set[A]]
//  def vector[A](of: F[A]): F[Vector[A]]
//  def seq[A](of: F[A]): F[Seq[A]]
//  def array[A](of: F[A]): F[Array[A]]
//  def map[V](value: F[V]): F[Map[String, V]]

  def pmap[A,B](fa: F[A])(f: A => Attempt[B])(g: B => A): F[B]
  def imap[A,B](fa: F[A])(f: A => B)(g: B => A): F[B]
}
