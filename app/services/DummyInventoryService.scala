package services

import javax.inject._
import scala.concurrent._
import scala.util._

import models.Product
import controllers.{QueryHandler, WriteService}


class DummyInventoryService @Inject()()(
  implicit ec: ExecutionContext
) extends QueryHandler with WriteService {

  var inventory: List[Product] = List(
    Product(0, "foo", 123.4),
    Product(1, "bar", 56.78)
  )

  private def prettyPrint = {
    val pretty: String = inventory
      .map(product => s"  $product")
      .mkString(",\n")
    val res = if (inventory.size == 0) "[]" else s"[\n$pretty\n]"
    println(s"inventory = $res\n")
  }

  def products: Future[List[Product]] = Future { inventory }

  def productById(id: Int): Future[Option[Product]] = Future {
    inventory.find(_.id == id)
  }

  def productByLabel(label: String): Future[Option[Product]] = Future {
    inventory.find(_.label == label)
  }

  def addProduct(label: String, price: Double): Future[Product] = Future {
    val newId: Int = inventory
      .lastOption
      .map(_.id + 1)
      .getOrElse(0)
    val newProduct = Product(newId, label, price)
    inventory :+= newProduct
    prettyPrint
    newProduct
  }

  def deleteProduct(id: Int): Future[Option[Product]] = Future {
    val i = inventory.indexWhere(_.id == id)
    if (i == -1) None
    else inventory.splitAt(i) match {
      case (before, toBeDeleted :: after) => {
        inventory = before ::: after
        prettyPrint
        Some(toBeDeleted)
      }
      case _ => None
    }
  }

  private def updateById(
    id: Int,
    update: Product => Product): Future[Option[Product]] = Future {

    val i = inventory.indexWhere(_.id == id)
    if (i == -1) None
    else {
      inventory = inventory.updated(i, update(inventory(i)))
      prettyPrint
      Some(inventory(i))
    }
  }

  def updateLabel(id: Int, newLabel: String): Future[Option[Product]] =
    updateById(id, old => Product(old.id, newLabel, old.price))

  def updatePrice(id: Int, newPrice: Double): Future[Option[Product]] =
    updateById(id, old => Product(old.id, old.label, newPrice))
}
