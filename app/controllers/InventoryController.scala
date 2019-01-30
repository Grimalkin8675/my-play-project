package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import scala.util._
import play.api.libs.json._

import models.Product


trait QueryHandler {
  def products: Future[List[Product]]
  def productById(id: Int): Future[Option[Product]]
  def productByLabel(label: String): Future[Option[Product]]
}

trait WriteService {
  def addProduct(label: String, price: Double): Future[Product]
  def deleteProduct(id: Int): Future[Option[Product]]
  def updateLabel(id: Int, label: String): Future[Option[Product]]
  def updatePrice(id: Int, price: Double): Future[Option[Product]]
}


@Singleton
class InventoryController @Inject()(
  queryHandler: QueryHandler,
  writeService: WriteService,
  cc: ControllerComponents
)(
  implicit ec: ExecutionContext
) extends AbstractController(cc) {

  def products = Action.async {
    queryHandler.products.map(products => Ok(Json.toJson(products)))
  }

  def product(label: String) = Action.async {
    queryHandler.productByLabel(label)
      .map(_
        .map(product => Ok(Json.toJson(product)))
        .getOrElse(NotFound(s"Product not found: $label")))
  }

  def addProduct = Action.async(parse.json) { request =>
    (for {
      label <- (request.body \ "label").validate[String].asOpt
      price <- (request.body \ "price").validate[Double].asOpt
    } yield writeService.addProduct(label, price))
      .map(_.map(id => Ok(Json.obj("id" -> id))))
      .getOrElse(Future { BadRequest("Invalid Product.") })
  }

  def deleteProduct(id: Int) = Action.async {
    writeService.deleteProduct(id)
      .map(_
        .map(product => Ok(Json.toJson(product)))
        .getOrElse(BadRequest(s"Product with id: $id")))
  }

  def updateLabel(id: Int) = Action.async(parse.json) { request =>
    (request.body \ "label").validate[String].asOpt
      .map(label =>
        writeService
          .updateLabel(id, label)
          .map(_
            .map(product => Ok(Json.toJson(product)))
            .getOrElse(BadRequest(s"No Product with id: $id"))))
      .getOrElse(Future { BadRequest("Invalid label") })
  }

  def updatePrice(id: Int) = Action.async(parse.json) { request =>
     (request.body \ "price").validate[Double].asOpt
      .map(price =>
        writeService
          .updatePrice(id, price)
          .map(_
            .map(product => Ok(Json.toJson(product)))
            .getOrElse(BadRequest(s"No Product with id: $id"))))
      .getOrElse(Future { BadRequest("Invalid price") })
  }
}
