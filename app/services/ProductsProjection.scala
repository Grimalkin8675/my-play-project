package services

import javax.inject._
import scala.concurrent._
import scala.annotation.tailrec

import models._
import controllers.QueryHandler


trait Subscribable {
  def subscribe(eventHandler: EventHandler): Unit
}

@Singleton
class ProductsProjection @Inject()(
  eventBus: Subscribable
)(
  implicit ec: ExecutionContext
) extends QueryHandler with EventHandler {

  eventBus.subscribe(this)

  var inventory: List[Product] = List.empty[Product]

  def products: Future[List[Product]] = Future { inventory }

  def productByLabel(label: String): Future[Option[Product]] = Future {
    inventory.find(_.label == label)
  }

  private def update(
    products: List[Product],
    id: Int,
    f: Product => Product): List[Product] = {

    @tailrec
    def updateRec(
      acc: List[Product],
      products: List[Product]): List[Product] = (acc, products) match {

      case (acc, Nil) => acc
      case (acc, product::tail) if product.id == id =>
        (acc :+ f(product)) ::: tail
      case (acc, product::tail) => updateRec(acc :+ product, tail)
    }

    updateRec(List.empty[Product], products)
  }

  def handleEvent(event: ProductEvent): Unit = {
    inventory = event match {
      case ProductAdded(product) => inventory :+ product
      case ProductDeleted(id) => inventory.filter(_.id != id)
      case ProductLabelUpdated(id, label) =>
        update(inventory, id, _.copy(label=label))
      case ProductPriceUpdated(id, price) =>
        update(inventory, id, _.copy(price=price))
    }
  }
}
