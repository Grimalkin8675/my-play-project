package models

import play.api.libs.json._


case class Product(id: Int, label: String, price: Double)

object Product {
  implicit val productWrites = Json.writes[Product]
}
