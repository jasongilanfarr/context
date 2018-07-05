package org.thisamericandream.context

import java.util.concurrent.CountDownLatch

import scala.concurrent.{Future, Promise}

class CancelContextTest extends Spec {

  "CancelContext" when {
    "there is no context" when {
      "not be cancelled" in {
        CancelContext.isCancelled should be(false)
      }
      "calling cancel will still not be cancelled" in {
        CancelContext.cancel(new UserCancelled[Unit](()))
        CancelContext.isCancelled should be(false)
      }
      "calling cancel will not call a listener" in {
        var called = false
        CancelContext.onCancel(_ => {
          called = true
        })
        CancelContext.cancel(new UserCancelled[Unit](()))
        called should be(false)
      }
    }
    "there is a context" when {
      "Not be cancelled" in {
        CancelContext.withCancellation(() => CancelContext.isCancelled should be(false))
      }
      "cancelling" should {
        "cancel" in {
          CancelContext.withCancellation { () =>
            CancelContext.cancel(new UserCancelled[Unit](()))
            CancelContext.isCancelled should be(true)
          }
        }
        "call the listeners" in {
          var called = false
          CancelContext.withCancellation { () =>
            CancelContext.onCancel(_ => {
              called = true
            })
            CancelContext.cancel(new UserCancelled[Unit](()))
          }
          called should be(true)
        }
        "be cancellable with a handle" in {
          val latch = new CountDownLatch(1)
          val (cancelled, ctx) = CancelContext.withCancellation { () =>
            Future {
              latch.await()
              CancelContext.isCancelled
            }
          }
          ctx.cancel(UserCancelled("User error"))
          latch.countDown()

          whenReady(cancelled) { _ should be(true) }
        }
        "when a listener is added after cancellation, it is called off thread" in {
          val tid = Thread.currentThread()
          val (_, ctx) = CancelContext.withCancellation(() => ())
          ctx.cancel(new UserCancelled[Unit](()))
          val actualTid = Promise[Thread]()
          ctx.onCancel { _ =>
            actualTid.success(Thread.currentThread())
          }
          whenReady(actualTid.future) { _ should not equal tid }
        }
        "cancelling a parent cancels the child" in {
          val (child, parent) = CancelContext.withCancellation(() => CancelContext.withCancellation(() => ())._2)
          parent.cancel(new UserCancelled[Unit](()))
          child.isCancelled should be(true)
        }
        "cancelling a child does not cancel the parent" in {
          val (child, parent) = CancelContext.withCancellation(() => CancelContext.withCancellation(() => ())._2)
          child.cancel(new UserCancelled[Unit](()))
          parent.isCancelled should be(false)
        }
      }
    }
  }
}
