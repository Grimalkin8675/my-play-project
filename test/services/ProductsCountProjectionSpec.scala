import org.scalatestplus.play._
import org.scalamock.scalatest.MockFactory

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

import services.{ProductsCountProjection, Subscribable}


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
}
