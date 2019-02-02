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

  def handleEvent(event: ProductEvent): Unit = ???
}
