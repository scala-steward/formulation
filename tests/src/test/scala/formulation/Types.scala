package formulation

import java.time.{Instant, LocalDateTime}

case class Generic[T](value: T)

object Generic {
  def codec[A](avroPart: Avro[A], default: Option[A] = None): Avro[Generic[A]] =
    record1("formulation", "Generic")(Generic.apply[A])("value" -> member(avroPart, _.value, default))
}

case class UserId(id: Int)

sealed trait BookingProcess { val disc: Int }

object BookingProcess {
  final case class DateSelected(disc: Int, datetime: LocalDateTime) extends BookingProcess
  final case class NotStarted(disc: Int) extends BookingProcess
  final case class Cancelled(disc: Int) extends BookingProcess

  implicit val dateDeselected: Avro[BookingProcess.DateSelected] =
    record2("formulation", "DateSelected")(BookingProcess.DateSelected.apply)(
      "disc" -> member(int.discriminator(1), _.disc),
      "datetime" -> member(localDateTime, _.datetime)
    )

  implicit val notStarted: Avro[BookingProcess.NotStarted] =
    record1("formulation", "NotStarted")(BookingProcess.NotStarted.apply)(
      "disc" -> member(int.discriminator(0), _.disc)
    )

  implicit val cancelled: Avro[BookingProcess.Cancelled] =
    record1("formulation", "Cancelled")(BookingProcess.Cancelled.apply)(
      "disc" -> member(int.discriminator(2), _.disc)
    )

  implicit val codec: Avro[BookingProcess] =
    (dateDeselected | notStarted | cancelled).as[BookingProcess]
}

sealed trait Event

object Event {
  case class Completed(at: Instant) extends Event
  case class Failed(at: Instant) extends Event
  case class Started(at: Instant) extends Event

  implicit val completed: Avro[Completed] = record1("event", "Completed")(Completed.apply)("at" -> member(instant, _.at))
  implicit val failed: Avro[Failed] = record1("event", "Failed")(Failed.apply)("at" -> member(instant, _.at))
  implicit val started: Avro[Started] = record1("event", "Started")(Started.apply)("at" -> member(instant, _.at))

  implicit val codec: Avro[Event] = (completed | failed | started).as[Event]
}