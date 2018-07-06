package akka.dispatch

import java.util.concurrent.{ExecutorService, RejectedExecutionException, ThreadFactory}

import akka.actor.ActorCell
import akka.event.Logging.Error
import com.typesafe.config.Config
import org.thisamericandream.context.{Context, ContextPropagatingExecutorService}
import java.util

import org.slf4j.MDC

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Dispatcher that propagates [[Context]] with akka actors, otherwise works
  * the same as the normal akka dispatcher.
  *
  * Utilizes akka internal APIs out of pure necessity.
  *
  * To use, add:
  * {{{
  *   akka.actor.default-dispatcher {
  *     type = "akka.dispatch.ContextAwareDispatcherConfigurator"
  *   }
  * }}}
  *
  * to your application.conf. Should also be able to be used as an independent dispatcher
  * configured the same as Akka's normal dispatcher (except for the type)
  */
class ContextAwareDispatcher(_configurator: MessageDispatcherConfigurator,
                             id: String,
                             throughput: Int,
                             throughputDeadlineTime: Duration,
                             executorServiceFactoryProvider: ExecutorServiceFactoryProvider,
                             shutdownTimeout: FiniteDuration)
    extends Dispatcher(
      _configurator,
      id,
      throughput,
      throughputDeadlineTime,
      (id: String, threadFactory: ThreadFactory) => {
        new ExecutorServiceFactory {
          override def createExecutorService: ExecutorService = {
            val factory = executorServiceFactoryProvider.createExecutorServiceFactory(id, threadFactory)
            ContextPropagatingExecutorService(factory.createExecutorService)
          }
        }
      },
      shutdownTimeout
    ) {

  case class Wrapper(message: Any, context: Context.ContextMap, mdc: Option[util.Map[String, String]])

  override protected[akka] def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {
    super
      .dispatch(receiver, invocation.copy(Wrapper(invocation.message, Context.get(), Option(MDC.getCopyOfContextMap))))
  }

  override protected[akka] def registerForExecution(mbox: Mailbox,
                                                    hasMessageHint: Boolean,
                                                    hasSystemMessageHint: Boolean): Boolean = {
    if (mbox.canBeScheduledForExecution(hasMessageHint, hasSystemMessageHint)) {
      if (mbox.setAsScheduled()) {
        try {
          super.executorService.execute(() => processMailbox(mbox))
          true
        } catch {
          case _: RejectedExecutionException ⇒
            try {
              super.executorService.execute(() => processMailbox(mbox))
              true
            } catch { //Retry once
              case e: RejectedExecutionException ⇒
                mbox.setAsIdle()
                eventStream.publish(Error(e, getClass.getName, getClass, "registerForExecution was rejected twice!"))
                throw e
            }
        }
      } else {
        false
      }
    } else {
      false
    }
  }

  @tailrec private def processMailboxRec(
      mbox: Mailbox,
      left: Int = math.max(throughput, 1),
      deadlineNs: Long =
        if (isThroughputDeadlineTimeDefined) System.nanoTime() + throughputDeadlineTime.toNanos else 0L): Unit = {
    if (mbox.shouldProcessMessage) {
      val next = Option(mbox.dequeue())
      if (next.isDefined) {
        if (Mailbox.debug) println(mbox.actor.self + " processing message " + next.get)
        next.get match {
          case env @ Envelope(Wrapper(message, context, mdc), _) =>
            Context.withContext(context, mdc)(() => mbox.actor.invoke(env.copy(message = message)))
          case env: Envelope =>
            mbox.actor.invoke(env)
        }
        if (Thread.interrupted()) {
          throw new InterruptedException("Interrupted while processing actor messages")
        }
        mbox.processAllSystemMessages()
        if (left > 1 && (!isThroughputDeadlineTimeDefined || (System.nanoTime() - deadlineNs) < 0)) {
          processMailboxRec(mbox, left - 1, deadlineNs)
        }
      }
    }
  }

  /** normally implemented in [[Mailbox#run]] but that is final... */
  private def processMailbox(mbox: Mailbox): Unit = {
    try {
      if (!mbox.isClosed) {
        mbox.processAllSystemMessages()
        processMailboxRec(mbox)
      }
    } finally {
      mbox.setAsIdle()
      registerForExecution(mbox, hasMessageHint = false, hasSystemMessageHint = false)
    }
  }
}

/** Configurator for creating [[akka.dispatch.ContextAwareDispatcher]].
  * Returns the same dispatcher instance for for each invocation
  * of the `dispatcher()` method.
  */
class ContextAwareDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
    extends MessageDispatcherConfigurator(config, prerequisites) {

  import akka.util.Helpers.ConfigOps

  private val instance = new ContextAwareDispatcher(
    this,
    config.getString("id"),
    config.getInt("throughput"),
    config.getNanosDuration("throughput-deadline-time"),
    configureExecutor(),
    config.getMillisDuration("shutdown-timeout")
  )

  override def dispatcher(): MessageDispatcher = instance
}
