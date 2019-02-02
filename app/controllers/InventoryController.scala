package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import scala.util._
import play.api.libs.json._

import models._


trait QueryHandler {
  def products: Future[List[Product]]
  def productByLabel(label: String): Future[Option[Product]]
}

trait CommandHandler {
  def handleCommand[Command <: ProductCommand](
    maybeId: Option[Int],
    command: Command): Future[Option[Product]]
}


@Singleton
class InventoryController @Inject()(
  queryHandler: QueryHandler,
  commandHandler: CommandHandler,
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
    } yield
      commandHandler.handleCommand(None, AddProduct(label, price))
        .map(_
          .map(product => Created(Json.toJson(product)))
          .getOrElse(BadRequest("Product couldn't be created"))))
      .getOrElse(Future(BadRequest("Invalid product")))
  }

  def deleteProduct(id: Int) = Action.async {
    commandHandler.handleCommand(Some(id), DeleteProduct)
      .map(_
        .map(product => Ok(Json.toJson(product)))
        .getOrElse(BadRequest(s"No product with id: $id")))
  }

  def updateLabel(id: Int) = Action.async(parse.json) { request =>
    (request.body \ "label").validate[String].asOpt
      .map(label =>
        commandHandler.handleCommand(Some(id), UpdateProductLabel(label))
          .map(_
            .map(product => Ok(Json.toJson(product)))
            .getOrElse(BadRequest(s"No product with id: $id"))))
      .getOrElse(Future(BadRequest("Invalid label")))
  }

  def updatePrice(id: Int) = Action.async(parse.json) { request =>
     (request.body \ "price").validate[Double].asOpt
      .map(price =>
        commandHandler.handleCommand(Some(id), UpdateProductPrice(price))
          .map(_
            .map(product => Ok(Json.toJson(product)))
            .getOrElse(BadRequest(s"No product with id: $id"))))
      .getOrElse(Future(BadRequest("Invalid price")))
  }
}
