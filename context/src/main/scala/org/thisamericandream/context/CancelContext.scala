package org.thisamericandream.context

import org.thisamericandream.context.concurrent.Atomic

import scala.collection.mutable

sealed trait CancelReason
case object TimedOut extends CancelReason
case class UserCancelled[T](reason: T) extends CancelReason

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
final class CancelContext private (private var cancelReason: Option[CancelReason] = None,
                                   private var listeners: Atomic[mutable.ArrayBuffer[CancelReason => Unit]] = Atomic(
                                     mutable.ArrayBuffer.empty)) {
  def cancel(reason: CancelReason): Unit = {
    if (cancelReason.isEmpty) {
      cancelReason = Some(reason)
      notifyListeners(reason)
    }
  }

  def isCancelled: Boolean = cancelReason.isDefined

  def reason: Option[CancelReason] = cancelReason

  def onCancel(listener: CancelReason => Unit): Unit = {
    // if a cancel listener was added and the context is already cancelled,
    // the caller likely doesn't expect to be called within it's own stack frame.
    cancelReason.fold[Unit](listeners(_ += listener))(reason => global.execute(() => Context.clearContext(() => listener(reason))))
  }

  private def notifyListeners(cancelReason: CancelReason): Unit = {
    Context.clearContext(() =>
    listeners { l =>
      l.foreach(_.apply(cancelReason))
      mutable.ArrayBuffer.empty[CancelReason => Unit]
    })
  }
}

object CancelContext {
  case object CancelKey extends ContextKey[CancelContext] {}

  def cancel(reason: CancelReason): Unit = {
    Context.get(CancelKey).foreach(_.cancel(reason))
  }

  def reason: Option[CancelReason] = {
    Context.get(CancelKey).flatMap(_.cancelReason)
  }

  def isCancelled: Boolean = {
    Context.get(CancelKey).exists(_.cancelReason.isDefined)
  }

  def onCancel(f: CancelReason => Unit): Unit = {
    Context.get(CancelKey).foreach(_.onCancel(f))
  }

  def withCancellation[R](f: () => R): (R, CancelContext) = {
    val ctx = new CancelContext()
    Context.get(CancelKey).foreach(_.onCancel(reason => ctx.cancel(reason)))
    (Context.withContext(CancelKey, ctx)(f), ctx)
  }
}
