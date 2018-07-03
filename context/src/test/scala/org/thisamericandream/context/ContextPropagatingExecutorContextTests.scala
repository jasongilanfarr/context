package org.thisamericandream.context

import java.util.concurrent.Executors

import scala.concurrent.{Future, Promise}

class ContextPropagatingExecutorContextTests extends Spec {
  case object Key extends ContextKey[String]

  "ContextPropagatingExecutorContext" should {
    "propagate the context" in {
      whenReady(Context.withContext(Key, "abc")(() => Future { Context.get(Key) }))(_.value should equal("abc"))
    }
    "restore the previous value" in {
      val executor = Executors.newSingleThreadExecutor()
      implicit val ctx = new ContextPropagatingExecutorService(executor)
      whenReady(Context.withContext(Key, "abc")(() => Future { Context.get(Key) }))(_.value should equal("abc"))
      val promise = Promise[Option[String]]()
      ctx.execute(() => promise.success(Context.get(Key)))
      whenReady(promise.future)(_ shouldBe None)
    }
  }
}
