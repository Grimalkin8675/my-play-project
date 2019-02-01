import org.scalatestplus.play._
import org.scalamock.scalatest.MockFactory

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.Predef.identity

import services.{ProductsProjection, Subscribable}
import models._


class ProductsProjectionSpec extends PlaySpec with MockFactory {
  def result[A](future: Future[A]): A = Await.result(future, 1 second)

  "ProductsProjection" should {

    "initialize with an empty inventory" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)

      service.inventory mustBe List.empty
    }

    "subscribe to eventBus" in {
      var subscribeCalled = false
      val stubEventBus = stub[Subscribable]
      (stubEventBus.subscribe _)
        .when(*) onCall(_ => subscribeCalled = true) once

      val service = new ProductsProjection(stubEventBus)

      subscribeCalled mustBe true
    }

  }

  "ProductsProjection.products" should {

    "return a list of products" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123))

      result(service.products) mustBe List(Product(0, "foo", 123))
    }

  }

  "ProductsProjection.productByLabel(label)" should {

    "return a product if label exists" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123), Product(1, "bar", 789))

      result(service.productByLabel("bar")) mustBe Some(Product(1, "bar", 789))
    }

    "return no product if label doesn't exist" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123), Product(1, "bar", 789))

      result(service.productByLabel("baz")) mustBe None
    }

  }

  "ProductsProjection.delete(products, id)" should {

    "return products if no product has id id" in {
      val products = List(Product(0, "foo", 123), Product(1, "bar", 789))

      ProductsProjection.delete(products, 2) mustBe products

      ProductsProjection.delete(List.empty, -1) mustBe List.empty
    }

    "return products with first product having id id removed" in {
      val products1 = List(
        Product(0, "foo", 123),
        Product(1, "bar", 789),
        Product(0, "baz", 456))

      ProductsProjection.delete(products1, 0) mustBe List(
        Product(1, "bar", 789), Product(0, "baz", 456))

      val products2 = List(
        Product(1, "bar", 789),
        Product(0, "baz", 456))

      ProductsProjection.delete(products2, 0) mustBe List(
        Product(1, "bar", 789))
    }


  }

  "ProductsProjection.update(products, id, f)" should {

    "return products if no product has id id" in {
      val products = List(Product(0, "foo", 123), Product(1, "bar", 789))

      ProductsProjection.update(products, 2, identity) mustBe products
    }

    "return products with f applied to product with id id" in {
      val products = List(Product(0, "foo", 123), Product(1, "bar", 789))
      val f = (product: Product) => product.copy(label="baz")

      ProductsProjection.update(products, 0, f) mustBe List(
        Product(0, "baz", 123), Product(1, "bar", 789))
    }

  }

  "ProductsProjection.handleEvent(event)" should {

    "add product on ProductAdded event" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123))
      service.handleEvent(ProductAdded(Product(1, "bar", 789)))

      service.inventory mustBe List(
        Product(0, "foo", 123), Product(1, "bar", 789))
    }

    "delete product on ProductDeleted event if product exists" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123))
      service.handleEvent(ProductDeleted(0))

      service.inventory mustBe List.empty
    }

    "delete only one product on ProductDeleted event" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123), Product(0, "bar", 789))
      service.handleEvent(ProductDeleted(0))

      service.inventory mustBe List(Product(0, "bar", 789))
    }

    "do nothing on ProductDeleted event if product doesn't exist" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123))
      service.handleEvent(ProductDeleted(1))

      service.inventory mustBe List(Product(0, "foo", 123))
    }

    "update product's label on ProductLabelUpdated if product exists" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123))
      service.handleEvent(ProductLabelUpdated(0, "bar"))

      service.inventory mustBe List(Product(0, "bar", 123))
    }

    "do nothing on ProductLabelUpdated if product doesn't exist" in {
      val stubEventBus = stub[Subscribable]
      val service = new ProductsProjection(stubEventBus)
      service.inventory = List(Product(0, "foo", 123))
      service.handleEvent(ProductLabelUpdated(1, "bar"))

      service.inventory mustBe List(Product(0, "foo", 123))
    }

  }
}
