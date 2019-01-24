package services

import javax.inject._

import models._


trait EventHandler {
  def handleEvent(event: ProductEvent): Unit
}

@Singleton
class EventBus() extends Publishable with Subscribable {
  var subscribers: Set[EventHandler] = Set.empty[EventHandler]

  def publish(event: ProductEvent): Unit =
    subscribers.foreach(_.handleEvent(event))

  def subscribe(eventHandler: EventHandler): Unit =
    subscribers += eventHandler
}
