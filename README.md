# Scala Play CQRS example

Scala Play training exercice with CQRS architecture.


## Use

    sbt run


## Routes

### `app/controllers/InventoryController.scala`

* GET `/products`: list all products
* GET `/product/:label`: find a product by label
* POST `/product`: add new product
* DELETE `/product/:id`: delete product with id `id`
* PUT `/product/:id/label`: change the `label` of the product with id `id`
* PUT `/product/:id/price`: change the `price` of the product with id `id`


## CQRS architecture

![CQRS architecture](./cqrs.png "CQRS architecture")


Source file: [cqrs.nomnoml](./cqrs.nomnoml)

[http://www.nomnoml.com/](http://www.nomnoml.com/)


Salut Axel,

C'est propre, ça couvre quasiment tout ce qu'on a vu. Tu as même réécrit des fonction tail recursive pour optimiser les updates.

Je vois ces axes d'amélioration que tu pourrais explorer:
1. Tu pourrais définir des commandes pour chacune de tes actions. Cela te permettrait notamment de changer ton interface WriteService en un CommandHandler, ou CommandService (pour reprendre les termes plus CQRS: Command pour l'écriture et Query pour la lecture) avec une interface comme ça:
    ```scala
    trait CommandHandler[CommandKind, State] {
      def handleCommand[Command <: CommandKind](maybeId: Option[Id], command: Command]: State
    }
    ```

    Tu aurais alors
    ```scala
    DummyInventoryService(...) extends CommandHandler[ProductCommand, Seq[Product]] ...
    ```

2. Aller plus loin avec l'event bus pour qu'il supporte une seconde projection, et ainsi voir comment se propagent des events sur plusieurs services.

Tiens moi au courant
