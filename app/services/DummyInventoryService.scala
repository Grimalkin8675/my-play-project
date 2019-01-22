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

  private def printPretty = {
    val pretty: String = this.inventory
      .map(product => s"  $product")
      .mkString(",\n")
    val res = if (this.inventory.size == 0) "[]" else s"[\n$pretty\n]"
    println(s"inventory = $res\n")
  }

  def products: Future[List[Product]] = Future { this.inventory }

  def productByName(name: String): Future[Option[Product]] = Future {
    this.inventory.find(_.label == name)
  }

  def addProduct(
    productLabel: String,
    productPrice: Double): Future[Int] = Future {

    val newId: Int = this.inventory
      .lastOption
      .map(_.id + 1)
      .getOrElse(0)
    this.inventory :+= Product(newId, productLabel, productPrice)
    this.printPretty
    newId
  }

  def deleteProduct(id: Int): Future[List[Product]] = Future {
    val (productsHavingId, newInventory) = this.inventory.partition(_.id == id)
    this.inventory = newInventory
    this.printPretty
    productsHavingId
  }

  private def updateById(
    id: Int,
    update: Product => Product): Future[Option[Product]] = Future {

    val i = this.inventory.indexWhere(_.id == id)
    if (i == -1) None
    else {
      this.inventory = this.inventory.updated(i, update(this.inventory(i)))
      this.printPretty
      Some(this.inventory(i))
    }
  }

  def updateLabel(id: Int, newLabel: String): Future[Option[Product]] =
    this.updateById(id, old => Product(old.id, newLabel, old.price))

  def updatePrice(id: Int, newPrice: Double): Future[Option[Product]] =
    this.updateById(id, old => Product(old.id, old.label, newPrice))
}
