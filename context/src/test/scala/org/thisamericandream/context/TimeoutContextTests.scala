package org.thisamericandream.context

import java.util.concurrent.ScheduledExecutorService

import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise, blocking}

class TimeoutContextTests extends Spec with BeforeAndAfterAll {
  private implicit val scheduler: ScheduledExecutorService = ContextExecutors.newScheduledThreadPool(1)

  override def afterAll(): Unit = {
    scheduler.shutdown()
  }

  "Timeout Context" should {
    "there is a timeout" when {
      "call the listener" in {
        val promise = Promise[Boolean]()
        val (_, ctx) = TimeoutContext.withTimeout(1.milli) { () => () }
        ctx.onTimeout(() => promise.success(true))
        whenReady(promise.future) (_ should be(true))
      }
      "the cancel context should have the correct cancel reason" in {
        val (result, ctx) = TimeoutContext.withTimeout(1.milli) { () =>
          Future {
            blocking {
              Thread.sleep(3)
            }
            CancelContext.reason
          }
        }
        whenReady(result)(_.value should be (TimedOut))
        ctx.isTimedOut should be(true)
      }
    }
  }
}
