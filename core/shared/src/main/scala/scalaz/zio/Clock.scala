// Copyright (C) 2018 John A. De Goes. All rights reserved.
package scalaz.zio

import java.util.concurrent.TimeUnit
import scalaz.zio.duration.Duration

trait Clock extends Serializable {
  def currentTime(unit: TimeUnit): IO[Nothing, Long]
  val nanoTime: IO[Nothing, Long]
  def sleep(length: Long, unit: TimeUnit): IO[Nothing, Unit]
}

object Clock {
  object Live extends Clock {
    final def currentTime(unit: TimeUnit): IO[Nothing, Long] =
      system.currentTimeMillis.map(l => unit.convert(l, TimeUnit.MILLISECONDS))

    final val nanoTime: IO[Nothing, Long] = system.nanoTime

    final def sleep(length: Long, unit: TimeUnit): IO[Nothing, Unit] =
      IO.sleep(Duration(length, unit))
  }

  case class Test(ref: Ref[Test.Data]) extends Clock {
    final def currentTime(unit: TimeUnit): IO[Nothing, Long] =
      ref.get.map(data => unit.convert(data.currentTimeMillis, TimeUnit.MILLISECONDS))

    final val nanoTime: IO[Nothing, Long] =
      ref.get.map(_.nanoTime)

    final def sleep(length: Long, unit: TimeUnit): IO[Nothing, Unit] =
      ref.update(_.adjust(length, unit).sleep(length, unit)).void
  }
  object Test {
    val Zero = Data(0, 0, Nil)

    case class Data(nanoTime: Long, currentTimeMillis: Long, sleeps0: List[(Long, TimeUnit)]) {
      lazy val sleeps = sleeps0.reverse

      final def adjust(length: Long, unit: TimeUnit): Data = {
        val nanos  = TimeUnit.NANOSECONDS.convert(length, unit)
        val millis = TimeUnit.MILLISECONDS.convert(length, unit)

        Data(nanoTime + nanos, currentTimeMillis + millis, sleeps0)
      }

      final def sleep(length: Long, unit: TimeUnit): Data =
        copy(sleeps0 = ((length, unit)) :: sleeps0)
    }
  }
}
