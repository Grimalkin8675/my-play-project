import org.scalatestplus.play._
import org.scalamock.scalatest.MockFactory
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application

import scala.reflect.ClassTag

import scala.concurrent._
import ExecutionContext.Implicits.global

import controllers.{InventoryController, QueryHandler, CommandHandler}
import models._


class InventoryControllerSpec extends PlaySpec with MockFactory {
  def fakeApplication: Application = new GuiceApplicationBuilder().build()

  def fakeApplication[A: ClassTag](binding: A): Application =
    new GuiceApplicationBuilder()
      .overrides(bind[A].to(binding))
      .build()

  "InventoryController.products" should {

    "return OK for GET /products" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _)
        .when () returns Future(List(Product(0, "foo", 12.3)))
      val app = fakeApplication(stubQueryHandler)

      val result = route(app, FakeRequest(GET, "/products")).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe
        Json.arr(
          Json.obj(
            "id" -> 0,
            "label" -> "foo",
            "price" -> 12.3))
    }

  }

  "InventoryController.product(label)" should {

    "return OK for GET /product/:label if product exists" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.productByLabel _)
        .when ("bar") returns Future(Some(Product(1, "bar", 1.23)))
      val app = fakeApplication(stubQueryHandler)

      val result = route(app, FakeRequest(GET, "/product/bar")).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe
        Json.obj(
          "id" -> 1,
          "label" -> "bar",
          "price" -> 1.23)
    }

    "return NOT_FOUND for GET /product/:label if products doesn't exist" in {
      val stubQueryHandler = stub[QueryHandler]
      stubQueryHandler.productByLabel _ when "bar" returns Future(None)
      val app = fakeApplication(stubQueryHandler)

      val result = route(app, FakeRequest(GET, "/product/bar")).get

      status(result) mustBe NOT_FOUND
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) mustBe "Product not found: bar"
    }

  }

  "InventoryController.addProduct" should {

    "return CREATED for POST /product if product is correctly formated" in {
      val stubCommandHandler = stub[CommandHandler]
      (stubCommandHandler.handleCommand _)
        .when (None, AddProduct("foo", 45.6))
        .returns (Future(Some(Product(0, "foo", 45.6))))
      val app = fakeApplication(stubCommandHandler)

      val json = Json.obj("label" -> "foo", "price" -> 45.6)
      val request = FakeRequest(POST, "/product").withJsonBody(json)
      val result = route(app, request).get

      status(result) mustBe CREATED
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe
        Json.obj(
          "id" -> 0,
          "label" -> "foo",
          "price" -> 45.6)
    }

    "return BAD_REQUEST for POST /product if product is incorrectly formated" in {
      val app = fakeApplication

      val json = JsString("not a product")
      val request = FakeRequest(POST, "/product").withJsonBody(json)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) mustBe "Invalid product"
    }

  }

  "InventoryController.deleteProduct(id)" should {

    "return OK for DELETE /product/:id if product exists" in {
      val stubCommandHandler = stub[CommandHandler]
      (stubCommandHandler.handleCommand _)
        .when (Some(42), DeleteProduct) returns Future(Some(Product(0, "", 0)))
      val app = fakeApplication(stubCommandHandler)

      val result = route(app, FakeRequest(DELETE, "/product/42")).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe
        Json.obj(
          "id" -> 0,
          "label" -> "",
          "price" -> 0d)
    }

    "return BAD_REQUEST for DELETE /product/:id if product doesn't exist" in {
      val stubCommandHandler = stub[CommandHandler]
      (stubCommandHandler.handleCommand _)
        .when (Some(21), DeleteProduct) returns Future(None)
      val app = fakeApplication(stubCommandHandler)

      val result = route(app, FakeRequest(DELETE, "/product/21")).get

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) mustBe "No product with id: 21"
    }

  }

  "InventoryController.updateLabel(id)" should {

    "return OK for PUT /product/:id/label if label is correctly formated and product exists" in {
      val stubCommandHandler = stub[CommandHandler]
      (stubCommandHandler.handleCommand _)
        .when (Some(7), UpdateProductLabel("foo"))
        .returns (Future(Some(Product(7, "foo", 7.89))))
      val app = fakeApplication(stubCommandHandler)

      val json = Json.obj("label" -> "foo")
      val request = FakeRequest(PUT, "/product/7/label").withJsonBody(json)
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe
        Json.obj(
          "id" -> 7,
          "label" -> "foo",
          "price" -> 7.89)
    }

    "return BAD_REQUEST for PUT /product/:id/label if label is correctly formated and product doesn't exist" in {
      val stubCommandHandler = stub[CommandHandler]
      (stubCommandHandler.handleCommand _)
        .when (Some(7), UpdateProductLabel("foo")) returns Future(None)
      val app = fakeApplication(stubCommandHandler)

      val json = Json.obj("label" -> "foo")
      val request = FakeRequest(PUT, "/product/7/label").withJsonBody(json)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) mustBe "No product with id: 7"
    }

    "return BAD_REQUEST for PUT /product/:id/label if label is incorrectly formated" in {
      val app = fakeApplication

      val json = Json.obj("label" -> 12)
      val request = FakeRequest(PUT, "/product/0/label").withJsonBody(json)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) mustBe "Invalid label"
    }

  }

  "InventoryController.updatePrice(id)" should {

    "return OK for PUT /product/:id/price if price is correctly formated and product exists" in {
      val stubCommandHandler = stub[CommandHandler]
      (stubCommandHandler.handleCommand _)
        .when (Some(3), UpdateProductPrice(1.23))
        .returns (Future(Some(Product(3, "", 1.23))))
      val app = fakeApplication(stubCommandHandler)

      val json = Json.obj("price" -> 1.23)
      val request = FakeRequest(PUT, "/product/3/price").withJsonBody(json)
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe
        Json.obj(
          "id" -> 3,
          "label" -> "",
          "price" -> 1.23)
    }

    "return BAD_REQUEST for PUT /product/:id/price if price is correctly formated and product doesn't exist" in {
      val stubCommandHandler = stub[CommandHandler]
      (stubCommandHandler.handleCommand _)
        .when (Some(5), UpdateProductPrice(45.6)) returns Future(None)
      val app = fakeApplication(stubCommandHandler)

      val json = Json.obj("price" -> 45.6)
      val request = FakeRequest(PUT, "/product/5/price").withJsonBody(json)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) mustBe "No product with id: 5"
    }

    "return BAD_REQUEST for PUT /product/:id/price if price is incorrectly formated" in {
      val app = fakeApplication

      val json = Json.obj("price" -> "foo")
      val request = FakeRequest(PUT, "/product/0/price").withJsonBody(json)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) mustBe "Invalid price"
    }

  }
}
