package org.thisamericandream.context

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import scala.concurrent.duration.FiniteDuration

final case class TimeoutContext private (private val cancelContext: CancelContext) {
  def isTimedOut: Boolean = cancelContext.reason.exists {
    case TimedOut => true
    case _        => false
  }

  def onTimeout(listener: () => Unit): Unit = {
    cancelContext.onCancel(_ => listener.apply())
  }
}

object TimeoutContext {
  private[context] case object TimeoutKey extends ContextKey[TimeoutContext]

  def isTimedOut: Boolean = {
    Context.get(TimeoutKey).exists(_.isTimedOut)
  }

  def onTimeout(listener: () => Unit): Unit = {
    Context.get(TimeoutKey).foreach(_.onTimeout(listener))
  }

  def withTimeout[R](timeout: FiniteDuration)(f: () => R)(
      implicit scheduler: ScheduledExecutorService): (R, TimeoutContext) = {
    val ((result, ctx), _) = CancelContext.withCancellation(() => {
      val cancelCtx = Context.get(CancelContext.CancelKey).get
      scheduler.schedule(new Runnable() {
        def run(): Unit = {
          Context.clearContext(() => cancelCtx.cancel(TimedOut))
        }
      }, timeout.toNanos, TimeUnit.NANOSECONDS)
      val ctx = new TimeoutContext(cancelCtx)
      (Context.withContext(TimeoutKey, ctx)(f), ctx)
    })

    (result, ctx)
  }
}
