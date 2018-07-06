package org.thisamericandream.context.akka

import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import org.thisamericandream.context.{Context, ContextKey, Spec, global}

import scala.concurrent.Promise
import scala.concurrent.duration._

class ContextAwareSchedulerTest extends Spec with BeforeAndAfterAll {
  val system = ActorSystem()

  case object Key extends ContextKey[String]

  override def afterAll(): Unit = {
    whenReady(system.terminate())(_ => ())
  }

  "ContextAwareScheduler" should {
    "propagate context" in {
      val promise = Promise[Option[String]]()
      Context.withContext(Key, "abc") { () =>
        system.scheduler.scheduleOnce(1.nano) {
          promise.success(Context.get(Key))
        }
      }
      whenReady(promise.future)(_.value should be("abc"))
    }
  }
}
