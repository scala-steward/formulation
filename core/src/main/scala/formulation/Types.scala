package formulation

import org.codehaus.jackson.JsonNode

import scala.util.Try
import shapeless.ops.coproduct.Align
import shapeless.{Coproduct, Generic, HList}


trait ~>[F[_], G[_]] {
  def apply[A](fa: F[A]): G[A]
}

trait InvariantFunctor[F[_]] {
  def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
}

trait Traverse[F[_]] {
  def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]
}

object Traverse {
  val listInstance: Traverse[List] = new Traverse[List] {
    def traverse[G[_], A, B](l: List[A])(f: A => G[B])(implicit G: Applicative[G]): G[List[B]] = {
      l.foldRight(G.pure(List.empty[B])) {
        (hd, init) => Applicative.map2(f(hd), init)(_ :: _)
      }
    }
  }
}

trait Applicative[F[_]] {
  def pure[A](a: A): F[A]
  def ap[A,B](fa: F[A])(f: F[A => B]): F[B]
}

object Applicative {
  private[formulation] def map2[F[_],A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C)(implicit F: Applicative[F]): F[C] =
    F.ap(fb)(F.ap(fa)(F.pure(f.curried)))

  private[formulation] def map3[F[_],A,B,C, D](fa: F[A], fb: F[B], fc: F[C])(f: (A, B, C) => D)(implicit F: Applicative[F]): F[D] =
    F.ap(fc)(F.ap(fb)(F.ap(fa)(F.pure(f.curried))))

  implicit val attempt: Applicative[Attempt] = new Applicative[Attempt] {
    override def pure[A](a: A): Attempt[A] = Attempt.Success(a)
    override def ap[A, B](fa: Attempt[A])(f: Attempt[A => B]): Attempt[B] = f.flatMap(ff => fa.map(ff))
  }
}


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
    case Attempt.Error(err) => None
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
  final case class Error(error: Throwable) extends Attempt[Nothing]

  def exception(ex: Throwable): Attempt[Nothing] = Attempt.Error(ex)
  def error(msg: String): Attempt[Nothing] = Attempt.Error(new Throwable(msg))
  def success[A](value: A): Attempt[A] = Attempt.Success(value)

  def fromTry[A](t: Try[A]): Attempt[A] = t match {
    case scala.util.Failure(err) => exception(err)
    case scala.util.Success(value) => success(value)
  }

  def fromOption[A](ifEmpty: String)(option: Option[A]): Attempt[A] = option match {
    case None => error(ifEmpty)
    case Some(value) => success(value)
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
                                                                            I: InvariantFunctor[F],
                                                                            G: Generic.Aux[O, Repr],
                                                                            TA: Align[Repr, I],
                                                                            FA: Align[I, Repr]): Transformer[F, I, O] = new Transformer[F, I, O] {
    override def apply(fi: F[I]): F[O] =
      I.imap(fi)(a => G.from(FA(a)))(b => TA(G.to(b)))
  }
}

final case class RecordFqdn(namespace: String, name: String)

final case class DecodeError(error: Throwable) extends Throwable(error)