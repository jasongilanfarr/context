package org.thisamericandream.context

import org.slf4j.MDC

import scala.concurrent.ExecutionContext

/** Execution Context that propagates the [[Context]] */
case class ContextPropagatingExecutionContext(ctx: ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = {
    val context = Context.get()
    val mdc = Option(MDC.getCopyOfContextMap)
    ctx.execute(() => {
      Context.withContext(context, mdc)(() => runnable.run())
    })
  }

  override def reportFailure(cause: Throwable): Unit = ctx.reportFailure(cause)
}
