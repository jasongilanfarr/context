package org.thisamericandream.context

import java.util.concurrent.{ExecutorService, Executors, ScheduledExecutorService, ThreadFactory}

/**
  * @see also [[java.util.concurrent.Executors]]
  */
object ContextExecutors {
  def newFixedThreadPool(nThreads: Int): ExecutorService =
    ContextPropagatingExecutorService(Executors.newFixedThreadPool(nThreads))

  def newWorkstealingPool(parallelism: Int): ExecutorService =
    ContextPropagatingExecutorService(Executors.newWorkStealingPool(parallelism))

  def newWorkstealingPool(): ExecutorService =
    ContextPropagatingExecutorService(Executors.newWorkStealingPool())

  def newFixedThreadPool(nThreads: Int, threadFactory: ThreadFactory): ExecutorService =
    ContextPropagatingExecutorService(Executors.newFixedThreadPool(nThreads, threadFactory))

  def newSingleThreadExecutor(): ExecutorService =
    ContextPropagatingExecutorService(Executors.newSingleThreadExecutor())

  def newSingleThreadExector(threadFactory: ThreadFactory): ExecutorService =
    ContextPropagatingExecutorService(Executors.newSingleThreadExecutor(threadFactory))

  def newSingleThreadScheduledExecutor(): ScheduledExecutorService =
    ContextPropagatingScheduledExecutorService(Executors.newSingleThreadScheduledExecutor())

  def newSingleThreadScheduledExecutor(threadFactory: ThreadFactory): ScheduledExecutorService =
    ContextPropagatingScheduledExecutorService(Executors.newSingleThreadScheduledExecutor(threadFactory))

  def newScheduledThreadPool(corePoolSize: Int): ScheduledExecutorService =
    ContextPropagatingScheduledExecutorService(Executors.newScheduledThreadPool(corePoolSize))

  def newScheduledThreadPool(corePoolSize: Int, threadFactory: ThreadFactory): ScheduledExecutorService =
    ContextPropagatingScheduledExecutorService(Executors.newScheduledThreadPool(corePoolSize, threadFactory))

}
