import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

import controllers.InventoryController
import services._
import models._


class AppSpec extends PlaySpec {
  def result[A](future: Future[A]): A =
    Await.result(future, 1 second)

  def jsonRequest(body: JsValue): FakeRequest[JsValue] =
    FakeRequest("", "", FakeHeaders(), body)

  "App" should {

    "correctly propagate events" in {

      // init
      val eventBus = new EventBus()
      val productsCounter = new ProductsCountProjection(eventBus)
      val products = new ProductsProjection(eventBus)
      val commandHandler = new EventsService(products, eventBus)
      val controller = new InventoryController(
        products,
        commandHandler,
        stubControllerComponents())

      commandHandler.events mustBe List.empty
      products.inventory mustBe List.empty
      productsCounter.counter mustBe 0

      // adding two products
      val json1 = Json.obj("label" -> "foo", "price" -> 123)
      val request1 = jsonRequest(json1)

      result(controller.addProduct.apply(request1))

      commandHandler.events mustBe
        List(
          ProductAdded(Product(0, "foo", 123)))
      products.inventory mustBe
        List(
          Product(0, "foo", 123))
      productsCounter.counter mustBe 1

      val json2 = Json.obj("label" -> "bar", "price" -> 45.6)
      val request2 = jsonRequest(json2)

      result(controller.addProduct.apply(request2))

      commandHandler.events mustBe
        List(
          ProductAdded(Product(0, "foo", 123)),
          ProductAdded(Product(1, "bar", 45.6)))
      products.inventory mustBe
        List(
          Product(0, "foo", 123),
          Product(1, "bar", 45.6))
      productsCounter.counter mustBe 2

      // updating label of product with id 1
      val json3 = Json.obj("label" -> "bar updated")
      val request3 = jsonRequest(json3)

      result(controller.updateLabel(1).apply(request3))

      commandHandler.events mustBe
        List(
          ProductAdded(Product(0, "foo", 123)),
          ProductAdded(Product(1, "bar", 45.6)),
          ProductLabelUpdated(1, "bar updated"))
      products.inventory mustBe
        List(
          Product(0, "foo", 123),
          Product(1, "bar updated", 45.6))
      productsCounter.counter mustBe 2

      // updating price of product with id 0
      val json4 = Json.obj("price" -> 7.89)
      val request4 = jsonRequest(json4)

      result(controller.updatePrice(0).apply(request4))

      commandHandler.events mustBe
        List(
          ProductAdded(Product(0, "foo", 123)),
          ProductAdded(Product(1, "bar", 45.6)),
          ProductLabelUpdated(1, "bar updated"),
          ProductPriceUpdated(0, 7.89))
      products.inventory mustBe
        List(
          Product(0, "foo", 7.89),
          Product(1, "bar updated", 45.6))
      productsCounter.counter mustBe 2


      // deleting product of id 0
      val request5 = FakeRequest()

      result(controller.deleteProduct(0).apply(request5))

      commandHandler.events mustBe
        List(
          ProductAdded(Product(0, "foo", 123)),
          ProductAdded(Product(1, "bar", 45.6)),
          ProductLabelUpdated(1, "bar updated"),
          ProductPriceUpdated(0, 7.89),
          ProductDeleted(0))
      products.inventory mustBe
        List(
          Product(1, "bar updated", 45.6))
      productsCounter.counter mustBe 1
    }

  }
}
