package modules

import play.api.{Environment, Configuration}
import play.api.inject.Module

import controllers.Inventoryable
import services.{DummyInventoryService, EventsService}


class ServicesModule extends Module {
  def bindings(env: Environment, conf: Configuration) = Seq(
    // bind[Inventoryable].to[DummyInventoryService]
    bind[Inventoryable].to[EventsService]
  )
}
