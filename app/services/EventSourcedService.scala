
sealed trait ProductEvent {
  def action(old: List[Product]): List[Product]
}

case class ProductAdded(product: Product) extends ProductEvent {
  def action(old: List[Product]) = old :+ product
}

case class ProductDeleted(id: Int) extends ProductEvent {
  def action(old: List[Product]) = old.filter(_.id != id)
}

sealed class ProductUpdated(
  id: Int,
  update: Product => Product) extends ProductEvent {
  def action(old: List[Product]) = {
    val i = old.indexWhere(_.id == id)
    if (i == -1) old else old.updated(i, update(old(i)))
  }
}

case class ProductLabelUpdated(
  id: Int,
  newLabel: String) extends ProductUpdated(
  id,
  old => Product(old.id, newLabel, old.price))

case class ProductPriceUpdated(
  id: Int,
  newPrice: Double) extends ProductUpdated(
  id,
  old => Product(old.id, old.label, newPrice))
