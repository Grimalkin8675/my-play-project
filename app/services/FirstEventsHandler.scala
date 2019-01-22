package services

import javax.inject._
import scala.concurrent._
import scala.annotation.tailrec

import models.Product


class FirstEventsHandler @Inject()()(
  implicit ec: ExecutionContext) extends EventsHandlerService {

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

  def inventory(events: List[ProductEvent]): Future[List[Product]] = Future {
    events.foldLeft(List.empty[Product]) {
      case (acc, ProductAdded(product)) => acc :+ product
      case (acc, ProductDeleted(id)) => acc.filter(_.id != id)
      case (acc, ProductLabelUpdated(id, label)) =>
        update(acc, id, _.copy(label=label))
      case (acc, ProductPriceUpdated(id, price)) =>
        update(acc, id, _.copy(price=price))
    }
  }
}
