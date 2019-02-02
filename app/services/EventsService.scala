package services

import javax.inject._
import scala.concurrent._
import scala.util._

import models._
import controllers.{CommandHandler, QueryHandler}


trait Publishable {
  def publish(event: ProductEvent): Unit
}

class EventsService @Inject()(
  queryHandler: QueryHandler,
  eventBus: Publishable
)(
  implicit ec: ExecutionContext
) extends CommandHandler {

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

  private def addProduct(
    label: String,
    price: Double): Future[Option[Product]] =
    nextId.flatMap { newId =>
      val newProduct = Product(newId, label, price)
      addEvent(ProductAdded(newProduct))
        .map(_ => Some(newProduct))
    }

  private def deleteProduct(id: Int): Future[Option[Product]] =
    productById(id).flatMap(_
      .map(product =>
        addEvent(ProductDeleted(id))
          .map(_ => Some(product)))
      .getOrElse(Future(None)))

  private def updateById(
    id: Int,
    event: ProductEvent): Future[Option[Product]] =
    productById(id).flatMap(_
      .map(_ =>
        addEvent(event)
          .flatMap(_ => productById(id)))
      .getOrElse(Future(None)))

  def handleCommand[Command <: ProductCommand](
    maybeId: Option[Int],
    command: Command): Future[Option[Product]] = (maybeId, command) match {
    case (None, AddProduct(label, price)) => addProduct(label, price)
    case (Some(id), DeleteProduct) => deleteProduct(id)
    case (Some(id), UpdateProductLabel(label)) =>
      updateById(id, ProductLabelUpdated(id, label))
    case (Some(id), UpdateProductPrice(price)) =>
      updateById(id, ProductPriceUpdated(id, price))
    case _ => Future(None)
  }
}
