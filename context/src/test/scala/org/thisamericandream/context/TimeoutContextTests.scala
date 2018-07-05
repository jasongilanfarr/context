package org.thisamericandream.context

import java.util.concurrent.{CountDownLatch, ScheduledExecutorService}

import scala.concurrent.{Future, blocking}
import scala.concurrent.duration._

class TimeoutContextTests extends Spec {
  private implicit val scheduler: ScheduledExecutorService = ContextExecutors.newScheduledThreadPool(2)

  "Timeout Context" should {
    "there is a timeout" when {
      "show up as timed out" in {
        val (result, ctx) = TimeoutContext.withTimeout(1.milli) { () =>
          Future {
            blocking {
              Thread.sleep(2)
            }
            TimeoutContext.isTimedOut
          }
        }
        whenReady(result)(_ should be(true))
        ctx.isTimedOut should be(true)
      }
      "call the listener" in {
        val latch = new CountDownLatch(1)
        val (_, ctx) = TimeoutContext.withTimeout(1.milli) { () => () }
        ctx.onTimeout(() => latch.countDown())
        latch.await()
      }
      "the cancel context should have the correct cancel reason" in {
        val (result, _) = TimeoutContext.withTimeout(1.milli) { () =>
          Future {
            blocking {
              Thread.sleep(3)
            }
            CancelContext.reason
          }
        }
        whenReady(result)(_.value should be (TimedOut))
      }
    }

  }
}
