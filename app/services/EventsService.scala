package services

import javax.inject._
import scala.concurrent._
import scala.util._

import models.Product
import controllers.Inventoryable


sealed trait ProductEvent

case class ProductAdded(product: Product) extends ProductEvent

case class ProductDeleted(id: Int) extends ProductEvent

sealed class ProductUpdated(
  id: Int,
  update: Product => Product) extends ProductEvent

case class ProductLabelUpdated(
  id: Int,
  newLabel: String) extends ProductUpdated(
  id,
  old => Product(old.id, newLabel, old.price))

case class ProductPriceUpdated(
  id: Int,
  newPrice: Double) extends ProductUpdated(
  id,
  old => Product(old.id, old.label, newPrice))


trait EventsHandlerService {
  def inventory(events: List[ProductEvent]): Future[List[Product]]
}

class EventsService @Inject()(
    eventsHandlerService: EventsHandlerService
  )(
    implicit ec: ExecutionContext
  ) extends Inventoryable {

  var events: List[ProductEvent] = List(
    ProductAdded(Product(0, "foo", 123.4)),
    ProductAdded(Product(1, "bar", 56.78))
  )

  private def addEvent(event: ProductEvent): Future[Unit] = Future {
    events :+= event
  }

  private def prettyPrint = {
    val pretty: String = events
      .map(product => s"  $product")
      .mkString(",\n")
    val res = if (events.size == 0) "[]" else s"[\n$pretty\n]"
    println(s"events = $res\n")
  }

  def products: Future[List[Product]] = eventsHandlerService.inventory(events)

  def productByLabel(label: String): Future[Option[Product]] =
    eventsHandlerService.inventory(events)
      .map(_.find(_.label == label))

  def addProduct(
    productLabel: String,
    productPrice: Double): Future[Int] = ???
  def deleteProduct(id: Int): Future[List[Product]] = ???
  def updateLabel(id: Int, label: String): Future[Option[Product]] = ???
  def updatePrice(id: Int, price: Double): Future[Option[Product]] = ???
}
