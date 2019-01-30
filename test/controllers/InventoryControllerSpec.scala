import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalamock.scalatest.MockFactory
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import scala.concurrent._
import ExecutionContext.Implicits.global

import controllers.{InventoryController, QueryHandler, WriteService}
import models.Product


class InventoryControllerSpec extends PlaySpec with MockFactory {

  "InventoryController GET" should {

    "return the products" in {
      val stubQueryHandler = stub[QueryHandler]
      (stubQueryHandler.products _) when() returns(Future {
        List(Product(0, "foo", 12)) })

      val writeServiceMock = mock[WriteService]

      val controller =
        new InventoryController(
          stubQueryHandler,
          writeServiceMock,
          stubControllerComponents())
      val result = controller.products.apply(FakeRequest())

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.arr(
        Json.obj(
          "id" -> 0,
          "label" -> "foo",
          "price" -> 12))
    }
  }

}
