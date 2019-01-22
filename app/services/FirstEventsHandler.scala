package services

import scala.concurrent._

import models.Product


class FirstEventsHandler() extends EventsHandlerService {
  def inventory(events: List[ProductEvent]): Future[List[Product]] = ???
}
