package org.thisamericandream.context

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Promise
import scala.concurrent.duration._

class TimeoutContextTests extends Spec with BeforeAndAfterAll {
  private implicit val scheduler: ScheduledExecutorService = ContextExecutors.newScheduledThreadPool(1)

  override def afterAll(): Unit = {
    scheduler.shutdown()
  }

  "Timeout Context" should {
    "there is a timeout" when {
      "call the listener" in {
        val promise = Promise[Boolean]()
        val (_, ctx) = TimeoutContext.withTimeout(1.nano) { () => () }
        ctx.onTimeout(() => promise.success(true))
        whenReady(promise.future) (_ should be(true))
        ctx.isTimedOut should be(true)
      }
      "the cancel context should have the correct cancel reason" in {
        val promise = Promise[Option[CancelReason]]()
        val (_, ctx) = TimeoutContext.withTimeout(1.nano) { () =>
          scheduler.schedule(() => promise.success(CancelContext.reason), 2L, TimeUnit.NANOSECONDS)
        }
        whenReady(promise.future)(_.value should be (TimedOut))
        ctx.isTimedOut should be(true)
      }
    }
  }
}
