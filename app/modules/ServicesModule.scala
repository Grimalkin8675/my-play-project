package modules

import play.api.{Environment, Configuration}
import play.api.inject.Module

import controllers.{CommandHandler, QueryHandler}
import services.{
  EventsService,
  ProductsProjection,
  EventBus,
  Publishable,
  Subscribable}


class ServicesModule extends Module {
  def bindings(env: Environment, conf: Configuration) = Seq(
    bind[CommandHandler].to[EventsService],
    bind[QueryHandler].to[ProductsProjection],
    bind[Publishable].to[EventBus],
    bind[Subscribable].to[EventBus]
  )
}
