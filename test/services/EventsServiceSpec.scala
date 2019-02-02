import org.scalatestplus.play._
import org.scalamock.scalatest.MockFactory

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

import controllers.QueryHandler
import services.{EventsService, Publishable}
import models._


class EventsServiceSpec extends PlaySpec with MockFactory {
  def result[A](future: Future[A]): A = Await.result(future, 1 second)

  "EventsService.nextId" should {

    "return 0 if queryHandler.products list is empty" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _)
        .when () returns Future(List.empty) once

      val stubPublishable = stub[Publishable]

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      result(eventsService.nextId) mustBe 0
    }

    "return last product of queryHandler.products' id + 1" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _)
        .when ()
        .returns(Future(List(Product(2, "", 0), Product(4, "", 0))))
        .once

      val stubPublishable = stub[Publishable]

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      result(eventsService.nextId) mustBe 5
    }

  }

  "EventsService.handleCommand(None, AddProduct(label, price))" should {

    "add ProductAdded event to the events list and return new added Product" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _)
        .when () returns Future(List.empty) once

      val stubPublishable = stub[Publishable]
      var publishedEvent: Option[ProductEvent] = None
      (stubPublishable.publish _)
        .when (*)
        .onCall((event: ProductEvent) => publishedEvent = Some(event)) once

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      eventsService.events mustBe List.empty

      val addedProduct = result(
        eventsService.handleCommand(None, AddProduct("foo", 42))).get

      eventsService.events mustBe List(ProductAdded(addedProduct))
      publishedEvent mustBe Some(ProductAdded(addedProduct))

      addedProduct.label mustBe "foo"
      addedProduct.price mustBe 42
    }

  }

  "EventsService.handleCommand(Some(id), DeleteProduct)" should {

    "add ProductDeleted event to the events list and return deleted Some(Product) if it exists" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _)
        .when ()
        .returns(
          Future(List(Product(3, "bar", 789), Product(6, "foo", 123))))
        .once

      val stubPublishable = stub[Publishable]
      var publishedEvent: Option[ProductEvent] = None
      (stubPublishable.publish _)
        .when (*)
        .onCall((event: ProductEvent) => publishedEvent = Some(event)) once

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val deletedProduct = result(
        eventsService.handleCommand(Some(6), DeleteProduct))

      eventsService.events mustBe List(ProductDeleted(6))
      publishedEvent mustBe Some(ProductDeleted(6))

      deletedProduct mustBe Some(Product(6, "foo", 123))
    }

    "not change events list and return None if it doesn't exist" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _)
        .when ()
        .returns(
          Future(List(Product(3, "bar", 789), Product(6, "foo", 123))))
        .once

      val stubPublishable = stub[Publishable]
      var publishedEvent: Option[ProductEvent] = None
      (stubPublishable.publish _)
        .when (*)
        .onCall((event: ProductEvent) => publishedEvent = Some(event))
        .never

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val deletedProduct = result(
        eventsService.handleCommand(Some(8), DeleteProduct))

      eventsService.events mustBe List.empty
      publishedEvent mustBe None

      deletedProduct mustBe None
    }

  }

  "EventsService.handleCommand(Some(id), UpdateProductLabel(label))" should {

    "add ProductLabelUpdated event to events list and return updated Some(Product) if it exists" in {
      val stubQueryHandler = stub[QueryHandler]
      inSequence {
        (stubQueryHandler.products _)
          .when ()
          .returns(
            Future(List(Product(3, "bar", 789), Product(6, "foo", 123))))
          .once
        (stubQueryHandler.products _)
          .when ()
          .returns(
            Future(List(Product(3, "bar", 789), Product(6, "baz", 123))))
          .once
      }

      val stubPublishable = stub[Publishable]
      var publishedEvent: Option[ProductEvent] = None
      (stubPublishable.publish _)
        .when (*)
        .onCall((event: ProductEvent) => publishedEvent = Some(event))
        .once

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val updatedProduct = result(
        eventsService.handleCommand(Some(6), UpdateProductLabel("baz")))

      eventsService.events mustBe List(ProductLabelUpdated(6, "baz"))
      publishedEvent mustBe Some(ProductLabelUpdated(6, "baz"))

      updatedProduct mustBe Some(Product(6, "baz", 123))
    }

    "not change events list and return None if it doesn't exist" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _)
        .when ()
        .returns(
          Future(List(Product(3, "bar", 789), Product(6, "foo", 123))))
        .once

      val stubPublishable = stub[Publishable]
      var publishedEvent: Option[ProductEvent] = None
      (stubPublishable.publish _)
        .when (*)
        .onCall((event: ProductEvent) => publishedEvent = Some(event))
        .never

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val updatedProduct = result(
        eventsService.handleCommand(Some(8), UpdateProductLabel("baz")))

      eventsService.events mustBe List.empty
      publishedEvent mustBe None

      updatedProduct mustBe None
    }

  }

  "EventsService.handleCommand(Some(id), UpdateProductPrice(price))" should {

    "add ProductPriceUpdated event to events list and return updated Some(Product) if it exists" in {
      val stubQueryHandler = stub[QueryHandler]
      inSequence {
        (stubQueryHandler.products _)
          .when ()
          .returns(
            Future(List(Product(3, "bar", 789), Product(6, "foo", 123))))
          .once
        (stubQueryHandler.products _)
          .when ()
          .returns(
            Future(List(Product(3, "bar", 789), Product(6, "foo", 456))))
          .once
      }

      val stubPublishable = stub[Publishable]
      var publishedEvent: Option[ProductEvent] = None
      (stubPublishable.publish _)
        .when (*)
        .onCall((event: ProductEvent) => publishedEvent = Some(event))
        .once

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val updatedProduct = result(
        eventsService.handleCommand(Some(6), UpdateProductPrice(456)))

      eventsService.events mustBe List(ProductPriceUpdated(6, 456))
      publishedEvent mustBe Some(ProductPriceUpdated(6, 456))

      updatedProduct mustBe Some(Product(6, "foo", 456))
    }

    "not change events list and return None if it doesn't exist" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _)
        .when ()
        .returns(
          Future(List(Product(3, "bar", 789), Product(6, "foo", 123))))
        .once

      val stubPublishable = stub[Publishable]
      var publishedEvent: Option[ProductEvent] = None
      (stubPublishable.publish _)
        .when (*)
        .onCall((event: ProductEvent) => publishedEvent = Some(event))
        .never

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val updatedProduct = result(
        eventsService.handleCommand(Some(8), UpdateProductPrice(456)))

      eventsService.events mustBe List.empty
      publishedEvent mustBe None

      updatedProduct mustBe None
    }

  }

  "EventsService.handleCommand" should {
    "return None for all other id/command combinations" in {
      val eventsService =
        new EventsService(stub[QueryHandler], stub[Publishable])

      List(
        eventsService.handleCommand(Some(0), AddProduct("foo", 123)),
        eventsService.handleCommand(None, DeleteProduct),
        eventsService.handleCommand(None, UpdateProductLabel("bar")),
        eventsService.handleCommand(None, UpdateProductPrice(456)))
      .foreach(result(_) mustBe None)
    }
  }
}
