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

  private def prettyPrint = {
    val pretty: String = events
      .map(product => s"  $product")
      .mkString(",\n")
    val res = if (events.size == 0) "[]" else s"[\n$pretty\n]"
    println(s"events = $res\n")
  }

  def addProduct(label: String, price: Double): Future[Product] =
    queryHandler.products
      .map(_
        .lastOption
        .map(_.id + 1)
        .getOrElse(0))
      .flatMap { newId =>
        val newProduct = Product(newId, label, price)
        addEvent(ProductAdded(newProduct))
          .map { _ =>
            prettyPrint
            newProduct
          }}

  def deleteProduct(id: Int): Future[Option[Product]] =
    queryHandler.productById(id)
      .map { maybeProduct =>
        prettyPrint
        maybeProduct
          .map { product =>
            addEvent(ProductDeleted(id))
            product
          }
      }

  private def updateById(
    id: Int,
    event: ProductEvent): Future[Option[Product]] =
    queryHandler.products.map(_.find(_.id == id))
      .flatMap {
        case Some(_) =>
          addEvent(event)
            .flatMap { _ =>
              prettyPrint
              queryHandler.products.map(_.find(_.id == id))
            }
        case None => Future {
          prettyPrint
          None
        }
      }

  def updateLabel(id: Int, label: String): Future[Option[Product]] =
    updateById(id, ProductLabelUpdated(id, label))

  def updatePrice(id: Int, price: Double): Future[Option[Product]] =
    updateById(id, ProductPriceUpdated(id, price))
}
