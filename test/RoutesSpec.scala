import org.scalatest.TestData
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalamock.scalatest.MockFactory
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent._
import ExecutionContext.Implicits.global

import controllers.QueryHandler


class RoutesSpec extends PlaySpec with GuiceOneAppPerTest with MockFactory {
  val mockQueryHandler = stub[QueryHandler]

  override def fakeApplication() = new GuiceApplicationBuilder()
    .overrides(bind[QueryHandler].to(mockQueryHandler))
    .build()

  "Routes " should {

    "send 200 on a valid request (/products)" in {
      (mockQueryHandler.products _) when() returns(Future { List() })

      val result = route(app, FakeRequest(GET, "/products")).get

      status(result) mustBe OK
    }

  }
}
