package org.thisamericandream.context.akka

import java.util.concurrent.ThreadFactory

import akka.actor.{Cancellable, LightArrayRevolverScheduler}
import akka.event.LoggingAdapter
import com.typesafe.config.Config
import org.slf4j.MDC
import org.thisamericandream.context.Context

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class ContextAwareScheduler(config: Config, logAdapter: LoggingAdapter, threadFactory: ThreadFactory)
    extends LightArrayRevolverScheduler(config, logAdapter, threadFactory) {
  override def schedule(initialDelay: FiniteDuration, delay: FiniteDuration, runnable: Runnable)(
      implicit executor: ExecutionContext): Cancellable = {
    val ctx = Context.get()
    val mdc = Option(MDC.getCopyOfContextMap)
    super.schedule(initialDelay, delay, () => Context.withContext(ctx, mdc)(() => runnable.run()))
  }

  override def scheduleOnce(delay: FiniteDuration, runnable: Runnable)(
      implicit executor: ExecutionContext): Cancellable = {
    val ctx = Context.get()
    val mdc = Option(MDC.getCopyOfContextMap)
    super.scheduleOnce(delay, () => Context.withContext(ctx, mdc)(() => runnable.run()))
  }
}
