import org.scalatestplus.play._
import org.scalamock.scalatest.MockFactory

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

import services.{ProductsCountProjection, Subscribable}
import models._


class ProductsCountProjectionSpec extends PlaySpec with MockFactory {
  def result[A](future: Future[A]): A = Await.result(future, 1 second)

  "ProductsCountProjection" should {

    "initialize with a 0 value counter" in {
      val service = new ProductsCountProjection(stub[Subscribable])

      service.counter mustBe 0
    }

    "subscribe to eventBus" in {
      var subscribeCalled = false
      val stubEventBus = stub[Subscribable]
      (stubEventBus.subscribe _)
        .when (*) onCall(_ => subscribeCalled = true) once

      val service = new ProductsCountProjection(stubEventBus)

      subscribeCalled mustBe true
    }

  }

  "ProductsProjection.handleEvent(event)" should {

    "increment counter on ProductAdded event" in {
      val service = new ProductsCountProjection(stub[Subscribable])
      service.counter = 10
      service.handleEvent(ProductAdded(Product(1, "foo", 789)))

      service.counter mustBe 11
    }

    "decrement counter on ProductDeleted event" in {
      val service = new ProductsCountProjection(stub[Subscribable])
      service.counter = 6
      service.handleEvent(ProductDeleted(23))

      service.counter mustBe 5
    }

  }
}
