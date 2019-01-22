package services

import javax.inject._
import scala.concurrent._
import scala.util._

import models.Product
import controllers.Inventoryable


class DummyInventoryService @Inject()()(
  implicit ec: ExecutionContext) extends Inventoryable {

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

  def productByLabel(label: String): Future[Option[Product]] = Future {
    inventory.find(_.label == label)
  }

  def addProduct(
    productLabel: String,
    productPrice: Double): Future[Int] = Future {

    val newId: Int = inventory
      .lastOption
      .map(_.id + 1)
      .getOrElse(0)
    inventory :+= Product(newId, productLabel, productPrice)
    prettyPrint
    newId
  }

  def deleteProduct(id: Int): Future[List[Product]] = Future {
    val (productsHavingId, newInventory) = inventory.partition(_.id == id)
    inventory = newInventory
    prettyPrint
    productsHavingId
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
