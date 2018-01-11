package formulation

import cats.{Applicative, Invariant, ~>}
import org.codehaus.jackson.JsonNode
import shapeless.ops.coproduct.Align
import shapeless.{Coproduct, Generic}

import scala.util.Try

sealed trait Attempt[+A] { self =>
  def flatMap[B](f: A => Attempt[B]): Attempt[B] = self match {
    case Attempt.Success(value) => f(value)
    case Attempt.Error(error) => Attempt.Error(error)
  }

  def map[B](f: A => B): Attempt[B] = self match {
    case Attempt.Success(value) => Attempt.Success(f(value))
    case Attempt.Error(error) => Attempt.Error(error)
  }

  def toEither: Either[Throwable, A] = self match {
    case Attempt.Success(value) => Right(value)
    case Attempt.Error(err) => Left(err)
  }

  def toOption: Option[A] = self match {
    case Attempt.Success(value) => Some(value)
    case Attempt.Error(_) => None
  }

  def orElse[B](f: => Attempt[B]): Attempt[Either[A, B]] = self match {
    case Attempt.Success(left) => Attempt.success(Left(left))
    case Attempt.Error(_) => f match {
      case Attempt.Success(right) => Attempt.success(Right(right))
      case Attempt.Error(error) => Attempt.Error(error)
    }
  }
}

object Attempt {
  final case class Success[A](value: A) extends Attempt[A]
  final case class Error(error: Throwable) extends Attempt[Nothing] {
    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case Error(other) => other.getMessage == error.getMessage
        case _ => false
      }
    }
  }

  def exception(ex: Throwable): Attempt[Nothing] = Attempt.Error(ex)
  def error(msg: String): Attempt[Nothing] = Attempt.Error(new Throwable(msg))
  def success[A](value: A): Attempt[A] = Attempt.Success(value)

  def fromTry[A](t: Try[A]): Attempt[A] = t match {
    case scala.util.Failure(err) => exception(err)
    case scala.util.Success(value) => success(value)
  }

  def fromEither[L, R](t: Either[Throwable, R]): Attempt[R] = t match {
    case Left(err) => exception(err)
    case Right(value) => success(value)
  }

  def fromOption[A](ifEmpty: String)(option: Option[A]): Attempt[A] = option match {
    case None => error(ifEmpty)
    case Some(value) => success(value)
  }

  def or[A](left: Attempt[A], right: => Attempt[A]): Attempt[A] =
    left match {
      case Attempt.Success(v) => Attempt.success(v)
      case Attempt.Error(_) => right
    }

  implicit val attempt: Applicative[Attempt] = new Applicative[Attempt] {
    override def pure[A](a: A): Attempt[A] = Attempt.Success(a)
    override def ap[A, B](ff: Attempt[A => B])(fa: Attempt[A]): Attempt[B] = ff.flatMap(f => fa.map(f))
  }
}

final case class Member[F[_], A, B] (typeClass: F[A], getter: B => A, defaultValue: Option[JsonNode] = None) {
  def mapTypeClass[G[_]](f: F ~> G): Member[G, A, B] = copy(typeClass = f(typeClass))
}


trait Transformer[F[_], I, O] {
  def apply(fi: F[I]): F[O]
}

object Transformer {
  implicit def alignedCoproduct[F[_], I <: Coproduct, Repr <: Coproduct, O](implicit
                                                                            I: Invariant[F],
                                                                            G: Generic.Aux[O, Repr],
                                                                            TA: Align[Repr, I],
                                                                            FA: Align[I, Repr]): Transformer[F, I, O] = new Transformer[F, I, O] {
    override def apply(fi: F[I]): F[O] =
      I.imap(fi)(a => G.from(FA(a)))(b => TA(G.to(b)))
  }
}

final case class RecordFqdn(namespace: String, name: String)

final case class DecodeError(error: Throwable) extends Throwable(error)