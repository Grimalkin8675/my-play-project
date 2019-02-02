package models


sealed trait ProductCommand

case class AddProduct(label: String, price: Double) extends ProductCommand

case object DeleteProduct extends ProductCommand

case class UpdateProductLabel(newLabel: String) extends ProductCommand

case class UpdateProductPrice(newPrice: Double) extends ProductCommand
