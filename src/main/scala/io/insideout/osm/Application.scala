/**
 * @author David Riccitelli
 */

package io.insideout.osm

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import spray.can.Http
import scala.slick.jdbc.meta.MTable
import io.insideout.osm.data.{Importers, DataNodes}
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

object Application extends App with Logger {

  // create tables.
  Database.forURL(Configuration.databaseURL, driver = Configuration.databaseDriver) withSession {
    if (0 == MTable.getTables(DataNodes.tableName).list.length) DataNodes.ddl.create
    if (0 == MTable.getTables(Importers.tableName).list.length) Importers.ddl.create
  }

    // we need an ActorSystem to host our application in
  implicit val system = ActorSystem()

  // create and start our service actor
  val service = system.actorOf(Props[WebServiceActor], "web-service")

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(service, "localhost", port = 8080)

}
