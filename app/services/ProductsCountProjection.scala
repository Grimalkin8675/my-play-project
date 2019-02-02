package services

import javax.inject._
import scala.concurrent._

import models._


@Singleton
class ProductsCountProjection @Inject()(
  eventBus: Subscribable
)(
  implicit ec: ExecutionContext
) extends EventHandler {

  eventBus.subscribe(this)

  var counter = 0

  def handleEvent(event: ProductEvent): Unit = event match {
    case ProductAdded(_) => counter += 1
    case ProductDeleted(_) => counter -= 1
    case _ => Unit
  }
}
