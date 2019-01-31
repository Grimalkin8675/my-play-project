import org.scalatestplus.play._
import org.scalamock.scalatest.MockFactory

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

import controllers.{WriteService, QueryHandler}
import services.{EventsService, Publishable}
import models._


class EventsServiceSpec extends PlaySpec with MockFactory {
  def result[A](future: Future[A]): A =
    Await.result(future, 1 second)

  "EventsService.nextId" should {

    "return 0 if queryHandler.products list is empty" in {
      val stubQueryHandler = stub[QueryHandler]
      stubQueryHandler.products _ when() returns Future(List.empty) once

      val stubPublishable = stub[Publishable]

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      result(eventsService.nextId) mustBe 0
    }

    "return last product of queryHandler.products' id + 1" in {
      val stubQueryHandler = stub[QueryHandler]
      stubQueryHandler.products _ when() returns Future(List(
        Product(2, "", 0),
        Product(4, "", 0))) once

      val stubPublishable = stub[Publishable]

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      result(eventsService.nextId) mustBe 5
    }

  }

  "EventsService.addProduct(label, price)" should {

    "add ProductAdded event to the events list and return new added Product" in {
      val stubQueryHandler = stub[QueryHandler]
      stubQueryHandler.products _ when() returns Future(List.empty) once

      val stubPublishable = stub[Publishable]
      var publishedEvents = List.empty[ProductEvent]
      (stubPublishable.publish _)
        .when(*) onCall((event: ProductEvent) => publishedEvents :+= event) once

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      eventsService.events mustBe List.empty

      val addedProduct = result(eventsService.addProduct("foo", 42))

      eventsService.events mustBe List(ProductAdded(addedProduct))
      publishedEvents mustBe List(ProductAdded(addedProduct))

      addedProduct.label mustBe "foo"
      addedProduct.price mustBe 42
    }

  }

  "EventsService.deleteProduct(id)" should {

    "add ProductDeleted event to the events list and return deleted Some(Product) if it exists" in {
      val stubQueryHandler = stub[QueryHandler]
      stubQueryHandler.products _ when() returns Future(List(
        Product(3, "bar", 789),
        Product(6, "foo", 123))) once

      val stubPublishable = stub[Publishable]
      var publishedEvents = List.empty[ProductEvent]
      (stubPublishable.publish _)
        .when(*) onCall((event: ProductEvent) => publishedEvents :+= event) once

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val deletedProduct = result(eventsService.deleteProduct(6))

      eventsService.events mustBe List(ProductDeleted(6))
      publishedEvents mustBe List(ProductDeleted(6))

      deletedProduct mustBe Some(Product(6, "foo", 123))
    }

    "not change events list and return None if it doesn't exist" in {
      val stubQueryHandler = stub[QueryHandler]
      stubQueryHandler.products _ when() returns Future(List(
        Product(3, "bar", 789),
        Product(6, "foo", 123))) once

      val stubPublishable = stub[Publishable]
      var publishedEvents = List.empty[ProductEvent]
      (stubPublishable.publish _)
        .when(*) onCall((event: ProductEvent) => publishedEvents :+= event) never

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val deletedProduct = result(eventsService.deleteProduct(8))

      eventsService.events mustBe List.empty
      publishedEvents mustBe List.empty

      deletedProduct mustBe None
    }

  }

  "EventsService.updateById(id, event)" should {

    "add event to events list and return updated Some(Product) if it exists" in {
      val stubQueryHandler = stub[QueryHandler]
      inSequence {
        stubQueryHandler.products _ when() returns Future(List(
          Product(3, "bar", 789),
          Product(6, "foo", 123))) once()
        stubQueryHandler.products _ when() returns Future(List(
          Product(3, "bar", 789),
          Product(6, "baz", 123))) once()
      }

      val stubPublishable = stub[Publishable]
      var publishedEvents = List.empty[ProductEvent]
      (stubPublishable.publish _)
        .when(*) onCall((event: ProductEvent) => publishedEvents :+= event) once

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val event = ProductLabelUpdated(-12, "baz")
      val updatedProduct = result(eventsService.updateById(6, event))

      eventsService.events mustBe List(event)
      publishedEvents mustBe List(event)

      updatedProduct mustBe Some(Product(6, "baz", 123))
    }

    "not change events list and return None if it doesn't exist" in {
      val stubQueryHandler = stub[QueryHandler]
      stubQueryHandler.products _ when() returns Future(List(
        Product(3, "bar", 789),
        Product(6, "foo", 123))) once

      val stubPublishable = stub[Publishable]
      var publishedEvents = List.empty[ProductEvent]
      (stubPublishable.publish _)
        .when(*) onCall((event: ProductEvent) => publishedEvents :+= event) never

      val stubEvent = stub[ProductPriceUpdated]

      val eventsService = new EventsService(stubQueryHandler, stubPublishable)

      val updatedProduct = result(eventsService.updateById(8, stubEvent))

      eventsService.events mustBe List.empty
      publishedEvents mustBe List.empty

      updatedProduct mustBe None
    }

  }
}
