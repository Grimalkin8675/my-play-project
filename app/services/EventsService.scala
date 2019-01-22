package services

import javax.inject._
import scala.concurrent._
import scala.util._

import models.Product
import controllers.{WriteService, ReadService}


sealed trait ProductEvent

case class ProductAdded(product: Product) extends ProductEvent

case class ProductDeleted(id: Int) extends ProductEvent

case class ProductLabelUpdated(id: Int, newLabel: String) extends ProductEvent

case class ProductPriceUpdated(id: Int, newPrice: Double) extends ProductEvent


trait Publishable {
  def publish(event: ProductEvent): Unit
}

class EventsService @Inject()(
  readService: ReadService,
  eventBus: Publishable
)(
  implicit ec: ExecutionContext
) extends WriteService {

  var events: List[ProductEvent] = List.empty[ProductEvent]

  private def addEvent(event: ProductEvent): Future[Unit] = Future {
    events :+= event
    eventBus.publish(event)
  }

  private def prettyPrint = {
    val pretty: String = events
      .map(product => s"  $product")
      .mkString(",\n")
    val res = if (events.size == 0) "[]" else s"[\n$pretty\n]"
    println(s"events = $res\n")
  }

  def addProduct(label: String, price: Double): Future[Int] =
    readService.products
      .map(_
        .lastOption
        .map(_.id + 1)
        .getOrElse(0))
      .flatMap(newId =>
        addEvent(ProductAdded(Product(newId, label, price)))
          .map(_ => {
            prettyPrint
            newId
          }))

  def deleteProduct(id: Int): Future[List[Product]] =
    readService.products
      .map(_.filter(_.id == id))
      .flatMap(toBeDeleted =>
        addEvent(ProductDeleted(id))
          .map(_ => {
            prettyPrint
            toBeDeleted
          }))

  private def updateById(
    id: Int,
    event: ProductEvent): Future[Option[Product]] =
    addEvent(event)
      .flatMap(_ => {
        prettyPrint
        readService.products
          .map(_.find(_.id == id))})

  def updateLabel(id: Int, label: String): Future[Option[Product]] =
    updateById(id, ProductLabelUpdated(id, label))

  def updatePrice(id: Int, price: Double): Future[Option[Product]] =
    updateById(id, ProductPriceUpdated(id, price))
}
