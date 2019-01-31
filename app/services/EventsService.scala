package services

import javax.inject._
import scala.concurrent._
import scala.util._

import models._
import controllers.{WriteService, QueryHandler}


trait Publishable {
  def publish(event: ProductEvent): Unit
}

class EventsService @Inject()(
  queryHandler: QueryHandler,
  eventBus: Publishable
)(
  implicit ec: ExecutionContext
) extends WriteService {

  var events: List[ProductEvent] = List.empty[ProductEvent]

  private def addEvent(event: ProductEvent): Future[Unit] = Future {
    events :+= event
    eventBus.publish(event)
  }

  private def productById(id: Int): Future[Option[Product]] =
    queryHandler.products.map(_.find(_.id == id))

  def nextId: Future[Int] =
    queryHandler.products.map(_
      .lastOption
      .map(_.id + 1)
      .getOrElse(0))

  def addProduct(label: String, price: Double): Future[Product] =
    nextId.flatMap { newId =>
      val newProduct = Product(newId, label, price)
      addEvent(ProductAdded(newProduct))
        .map(_ => newProduct)
    }

  def deleteProduct(id: Int): Future[Option[Product]] =
    productById(id).flatMap(_
      .map(product =>
        addEvent(ProductDeleted(id))
          .map(_ => Some(product)))
      .getOrElse(Future(None)))

  def updateById(
    id: Int,
    event: ProductEvent): Future[Option[Product]] =
    productById(id).flatMap(_
      .map(_ =>
        addEvent(event)
          .flatMap(_ => productById(id)))
      .getOrElse(Future(None)))

  def updateLabel(id: Int, label: String): Future[Option[Product]] =
    updateById(id, ProductLabelUpdated(id, label))

  def updatePrice(id: Int, price: Double): Future[Option[Product]] =
    updateById(id, ProductPriceUpdated(id, price))
}
