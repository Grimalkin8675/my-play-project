package models


sealed trait ProductEvent

case class ProductAdded(product: Product) extends ProductEvent

case class ProductDeleted(id: Int) extends ProductEvent

case class ProductLabelUpdated(id: Int, newLabel: String) extends ProductEvent

case class ProductPriceUpdated(id: Int, newPrice: Double) extends ProductEvent
