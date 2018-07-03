package org.thisamericandream.context

import org.thisamericandream.context.concurrent.Atomic

import scala.collection.mutable

/** Cancel Context provides <b>opt-in</b> cancellation support.
  * Unlike other cancellation libraries which tend to either use InterruptedException
  * or simply return to the caller/throw with a Cancellation Error and continue computing,
  * this library provides a safe, opt-in method:
  *
  * Any task can check whether or not cancellation was requested (e.g. at a safe point) and if
  * it was cancelled, can cleanup, undo, or whatever is necessary to safely cancel. It is up to the task itself to
  * determine how to report this to caller.
  *
  * Cancellation listener support is provided to support things like event loops or other alternatives where
  * checking the context is non-trivial.
  */
final class CancelContext private (private var cancelled: Boolean = false,
                                   private var listeners: Atomic[mutable.ArrayBuffer[() => Unit]] = Atomic(
                                     mutable.ArrayBuffer.empty)) {
  def cancel(): Unit = {
    if (!cancelled) {
      cancelled = true
      notifyListeners()
    }
  }

  def isCancelled: Boolean = cancelled

  def onCancel(l: () => Unit): Unit = {
    if (cancelled) {
      // if a cancel listener was added and the context is already cancelled,
      // the caller likely deosn't expect to be called within it's own stack frame.
      global.execute(() => l())
    } else {
      listeners(_ += l)
    }
  }

  private def notifyListeners(): Unit = {
    listeners { l =>
      l.foreach(_.apply())
      mutable.ArrayBuffer.empty[() => Unit]
    }
  }
}

object CancelContext {
  private[context] val key = new ContextKey[CancelContext] {}

  def cancel(): Unit = {
    Context.get(key).foreach(_.cancel())
  }

  def isCancelled: Boolean = {
    Context.get(key).exists(_.cancelled)
  }

  def onCancel(f: () => Unit): Unit = {
    Context.get(key).foreach(_.onCancel(f))
  }

  def withCancellation[R](f: () => R): (R, CancelContext) = {
    val ctx = new CancelContext()
    Context.get(key).foreach(_.onCancel(() => ctx.cancel()))
    (Context.withContext(key, ctx)(f), ctx)
  }
}
