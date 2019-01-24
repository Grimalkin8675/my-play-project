package services

import javax.inject._
import scala.concurrent._
import scala.util._

import models._
import controllers.{WriteService, ReadService}


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
          .map { _ =>
            prettyPrint
            newId
          })

  def deleteProduct(id: Int): Future[List[Product]] =
    readService.products.map(_.filter(_.id == id))
      .flatMap {
        case toBeDeleted if toBeDeleted.size != 0 =>
          addEvent(ProductDeleted(id))
            .map { _ =>
              prettyPrint
              toBeDeleted
            }
        case _ => Future {
          prettyPrint
          List.empty[Product]
        }
      }

  private def updateById(
    id: Int,
    event: ProductEvent): Future[Option[Product]] =
    readService.products.map(_.find(_.id == id))
      .flatMap {
        case Some(_) =>
          addEvent(event)
            .flatMap { _ =>
              prettyPrint
              readService.products.map(_.find(_.id == id))
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
