/**
 * @author David Riccitelli
 */
package io.insideout.osm

import spray.routing.HttpService
import akka.actor.{Props, Actor, ActorLogging}
import io.insideout.osm.data.{DataAccess, Importers}
import scala.slick.session.Session

/**
 *
 * @constructor
 */
class WebServiceActor extends Actor with ActorLogging with WebService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing,
  // timeout handling or alternative handler registration
  def receive = runRoute(route)

}

trait WebService extends HttpService with Logger {

  implicit def executionContext = actorRefFactory.dispatcher
  val importRef = actorRefFactory.actorOf(Props[ImportServiceActor], "import-service")

  val route = path("import" / Rest) { path =>
    get {
      complete {
        startImport(path)
      }
    }
  }

  def startImport(name: String) = DataAccess.database.withSession { implicit session: Session =>
    Importers.findOneByName(name) match {
      case Some(importer) => {
        importRef ! Import(importer.url)
        "import started"
      }
      case _ => "unknown importer"
    }
  }

}