import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.TestProbe
import org.scalatest.BeforeAndAfterAll
import org.thisamericandream.context.{Context, ContextKey, Spec}

case object Key extends ContextKey[String]

case class Get(sender: ActorRef)
case class ForwardGet(sendTo: ActorRef, msg: Get)

class ContextActor extends Actor {
  override def receive: Receive = {
    case Get(sender) =>
      sender ! Context.get(Key)
    case ForwardGet(to, msg) =>
      to ! msg
  }
}

class ContextActorTest extends Spec with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem()

  override def afterAll() {
    system.terminate()
  }

  val actor: ActorRef = system.actorOf(Props(classOf[ContextActor]))
  val probe: TestProbe = TestProbe()

  "ContextAwareDispatcher" should {
    "include the context in the message" in {
      Context.withContext(Key, "abc") { () =>
        actor ! Get(probe.ref)
      }
      probe.expectMsg(Some("abc"))
    }
    "clear the context after" in {
      Context.withContext(Key, "abc") { () =>
        actor ! Get(probe.ref)
      }
      actor ! Get(probe.ref)
      probe.expectMsg(Some("abc"))
      probe.expectMsg(None)
    }
    "forward the message" in {
      val next = system.actorOf(Props(classOf[ContextActor]))
      Context.withContext(Key, "abc") { () =>
        actor ! ForwardGet(next, Get(probe.ref))
      }
      probe.expectMsg(Some("abc"))
    }
  }
}
