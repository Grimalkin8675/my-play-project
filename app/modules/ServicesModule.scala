package modules

import play.api.{Environment, Configuration}
import play.api.inject.Module

import controllers.{WriteService, QueryHandler}
import services.{
  EventsService,
  ProductsProjection,
  EventBus,
  Publishable,
  Subscribable}


class ServicesModule extends Module {
  def bindings(env: Environment, conf: Configuration) = Seq(
    bind[WriteService].to[EventsService],
    bind[QueryHandler].to[ProductsProjection],
    bind[Publishable].to[EventBus],
    bind[Subscribable].to[EventBus]
  )
}
