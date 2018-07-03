package org.thisamericandream.context

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
        val child = Context.withContext(Key, "child") { () => Context.get(Key) }
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
  }
}
