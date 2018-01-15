package formulation

import eu.timepit.refined.api.{RefType, Refined, Validate}

package object refined {

  implicit class RichAvroRefined[T](val avro: Avro[T]) {
    def refine[P](implicit V: Validate[T, P], R: RefType[Refined]): Avro[Refined[T, P]] =
      avro.pmap[Refined[T, P]](p => R.refine(p).left.map(err => new Throwable(err)))(R.unwrap)
  }
}
