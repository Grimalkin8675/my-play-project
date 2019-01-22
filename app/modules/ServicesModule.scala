package modules

import play.api.{Environment, Configuration}
import play.api.inject.Module

import controllers.{WriteService, ReadService}
import services.{
  DummyInventoryService,
  EventsService,
  ProductsProjection,
  EventBus,
  Publishable,
  Subscribable}


class ServicesModule extends Module {
  def bindings(env: Environment, conf: Configuration) = Seq(
    // bind[WriteService].to[DummyInventoryService],
    // bind[ReadService].to[DummyInventoryService],
    bind[WriteService].to[EventsService],
    bind[ReadService].to[ProductsProjection],
    bind[Publishable].to[EventBus],
    bind[Subscribable].to[EventBus]
  )
}
