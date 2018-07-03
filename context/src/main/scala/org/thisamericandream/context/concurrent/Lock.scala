package org.thisamericandream.context.concurrent

import java.util.concurrent.locks.ReentrantLock

/**
  * RAII Lock object
  *
  * {{{
  *   val lock = Lock()
  *   lock {
  *     // protected by the lock (exception safe)
  *   }
  * }}}
  */
case class Lock private (private val l: ReentrantLock) {
  def apply[T](f: () => T): T = {
    l.lock()
    try {
      f()
    } finally {
      l.unlock()
    }
  }

  def lock[T](f: () => T): T = apply(f)
}

object Lock {
  def apply(): Lock = new Lock(new ReentrantLock())

  /**
    * Java API
    */
  def create(): Lock = apply()
}
