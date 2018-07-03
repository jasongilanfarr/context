package org.thisamericandream.context

import scala.concurrent.ExecutionContext

/** Execution Context that propagates the [[Context]] */
case class ContextPropagatingExecutionContext(ctx: ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = {
    val context = Context.get()
    ctx.execute(() => {
      Context.withContext(context)(() => runnable.run())
    })
  }

  override def reportFailure(cause: Throwable): Unit = ctx.reportFailure(cause)
}
