package formulation

import cats._

final case class Member[F[_], A, B](typeClass: F[A], getter: B => A, defaultValue: Option[A] = None) {
  def mapTypeClass[G[_]](f: F ~> G): Member[G, A, B] = copy(typeClass = f(typeClass))
}