package org.thisamericandream.context

import java.util
import java.util.concurrent._

/** ExecutorService that propagates the [[Context]] */
case class ContextPropagatingExecutorService(service: ExecutorService) extends AbstractExecutorService {
  override def shutdown(): Unit = service.shutdown()

  override def shutdownNow(): util.List[Runnable] = service.shutdownNow()

  override def isShutdown: Boolean = service.isShutdown

  override def isTerminated: Boolean = service.isTerminated

  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = service.awaitTermination(timeout, unit)

  override def execute(command: Runnable): Unit = {
    val ctx = Context.get()
    service.execute(() => {
      Context.withContext(ctx)(() => command.run())
    })
  }
}

/** Scheduled Executor Service that propagates [[Context]] */
case class ContextPropagatingScheduledScheduledExecutorService(service: ScheduledExecutorService)
    extends AbstractExecutorService
    with ScheduledExecutorService {
  override def schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture[_] =
    scheduleWithFixedDelay(command, 0, delay, unit)

  override def schedule[V](callable: Callable[V], delay: Long, unit: TimeUnit): ScheduledFuture[V] =
    scheduleWithFixedDelay(() => callable.call(), 0, delay, unit).asInstanceOf[ScheduledFuture[V]]

  override def scheduleAtFixedRate(command: Runnable,
                                   initialDelay: Long,
                                   period: Long,
                                   unit: TimeUnit): ScheduledFuture[_] = {
    val ctx = Context.get()
    service.scheduleAtFixedRate(() => Context.withContext(ctx)(() => command.run()), initialDelay, period, unit)
  }

  override def scheduleWithFixedDelay(command: Runnable,
                                      initialDelay: Long,
                                      delay: Long,
                                      unit: TimeUnit): ScheduledFuture[_] = {
    val ctx = Context.get()
    service.scheduleWithFixedDelay(() => Context.withContext(ctx)(() => command.run()), initialDelay, delay, unit)
  }

  override def shutdown(): Unit = service.shutdown()

  override def shutdownNow(): util.List[Runnable] = service.shutdownNow()

  override def isShutdown: Boolean = service.isShutdown

  override def isTerminated: Boolean = service.isTerminated

  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = service.awaitTermination(timeout, unit)

  override def execute(command: Runnable): Unit = {
    val ctx = Context.get()
    service.execute(() => Context.withContext(ctx)(() => command.run()))
  }
}
