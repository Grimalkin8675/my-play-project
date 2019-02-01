import org.scalatestplus.play._
import org.scalamock.scalatest.MockFactory

import services.{EventBus, EventHandler}
import models._


class EventBusSpec extends PlaySpec with MockFactory {

  "EventBus.subscriber(eventHandler)" should {

    "add handleEvent(eventHandler) to list of subscribers" in {
      val stubSubscriber1 = stub[EventHandler]
      val stubSubscriber2 = stub[EventHandler]

      val eventBus = new EventBus()
      eventBus.subscribers mustBe Set.empty

      eventBus.subscribe(stubSubscriber1)
      eventBus.subscribers mustBe Set(stubSubscriber1)

      eventBus.subscribe(stubSubscriber2)
      eventBus.subscribers mustBe Set(stubSubscriber1, stubSubscriber2)
    }

  }

  "EventBus.publish(event)" should {

    "call handleEvent(event) for each subscriber" in {
      var handledEvents1 = List.empty[ProductEvent]
      val stubSubscriber1 = stub[EventHandler]
      (stubSubscriber1.handleEvent _)
        .when(*) onCall((event: ProductEvent) => handledEvents1 :+= event) twice

      var handledEvents2 = List.empty[ProductEvent]
      val stubSubscriber2 = stub[EventHandler]
      (stubSubscriber2.handleEvent _)
        .when(*) onCall((event: ProductEvent) => handledEvents2 :+= event) twice

      val eventBus = new EventBus()
      eventBus.subscribe(stubSubscriber1)
      eventBus.subscribe(stubSubscriber2)

      val event1 = ProductAdded(Product(0, "foo", 123))
      val event2 = ProductDeleted(0)

      eventBus.publish(event1)

      handledEvents1 mustBe List(event1)
      handledEvents2 mustBe List(event1)

      eventBus.publish(event2)

      handledEvents1 mustBe List(event1, event2)
      handledEvents2 mustBe List(event1, event2)
    }

  }

}
