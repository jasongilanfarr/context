package org.thisamericandream.context

import org.slf4j.MDC

import scala.concurrent.{Future, Promise}

class ContextTests extends Spec {
  case object Key extends ContextKey[String]

  "Context" when {
    "empty" should {
      "have no value for the test context" in {
        Context.get(Key) shouldBe None
      }
    }
    "have a value when set" in {
      val result = Context.withContext(Key, "abc")(() => Context.get(Key))
      result.value should equal("abc")
    }
    "restore the previous value" in {
      val (childResult, parentResult) = Context.withContext(Key, "parent") { () =>
        val child = Context.withContext(Key, "child") { () =>
          Context.get(Key)
        }
        (child, Context.get(Key))
      }
      childResult.value should equal("child")
      parentResult.value should equal("parent")
    }
    "clearing the context should restore the context outside the scope" in {
      val (childResult, parentResult) = Context.withContext(Key, "parent") { () =>
        val child = Context.clearContext(() => Context.get(Key))
        (child, Context.get(Key))
      }
      childResult shouldBe None
      parentResult.value should equal("parent")
    }
    "propagate MDC" in {
      MDC.put("mdcKey", "key")
      val promise = Promise[Option[String]]()
      Future {
        promise.success(Option(MDC.get("mdcKey")))
      }
      whenReady(promise.future)(_.value should be("key"))
    }
    "clearing context clears MDC" in {
      MDC.put("mdcKey", "key")
      val promise = Promise[Option[String]]()
      Context.clearContext(() =>
        Future {
          promise.success(Option(MDC.get("mdcKey")))
      })
      whenReady(promise.future)(_ shouldBe None)
    }
  }
}
