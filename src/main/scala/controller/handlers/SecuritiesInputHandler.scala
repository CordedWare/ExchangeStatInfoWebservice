package controller.handlers
import _root_.entity.SecurityHub
import akka.actor.TypedActor.dispatcher
import config.AppServer.{db, securitiesHubTable}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Future

class SecuritiesInputHandler {

  def securitiesHandler(secid: String,
                        regnumber: String,
                        name: String,
                        emitentTitle: String): Future[String] = {
    val securitie = SecurityHub(None, secid, regnumber, name, emitentTitle)
    val insertAction = securitiesHubTable returning securitiesHubTable.map(_.id) into ((securitie, id) => securitie.copy(id = Some(id))) += securitie
    val future = db.run(insertAction)
    future.map(insertedSecuritie => s"Сохранено с ID ${insertedSecuritie.id.getOrElse("")}")
  }
}