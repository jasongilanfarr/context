package org.thisamericandream.context.concurrent

/**
  * Provides thread-safe access/modification of a given value
  * {{{
  *   val x = Atomic(0)
  *   x { _ + 1 } == 1
  *   x.foreach(println(_)) // prints "1"
  * }}}
  */
class Atomic[T] private (private[this] var value: T) {
  val lock = Lock()

  def apply[R <: T](f: T => R): R = {
    val result = lock(() => f(value))
    value = result
    result
  }

  def lock[R <: T](f: T => R): R = apply(f)

  def foreach[R](f: T => R): R = {
    lock(() => f(value))
  }
}

object Atomic {
  def apply[T](t: T): Atomic[T] = new Atomic(t)

  /**
    * Java API
    */
  def create[T](t: T): Atomic[T] = apply(t)
}
