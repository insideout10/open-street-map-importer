/**
 * @author David Riccitelli
 */
package io.insideout.osm

import scala.xml.XML
import io.insideout.osm.models.{DataNode, DataSet}
import io.insideout.osm.data.DataNodes
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import akka.actor._
import akka.routing.RoundRobinRouter
import io.insideout.osm.masterworker.Master
import io.insideout.osm.masterworker.MasterWorkerProtocol.{AllWorkSent, AllWorkCompleted}
import scala.Some

sealed case class Import(url: String)

class ImportServiceActor extends Actor with ActorLogging with ImportService {

  def actorRef = self
  def actorRefFactory = context

  def importing: Receive = {
    case AllWorkCompleted =>
      closeChangeset()
      context.become(receive)
  }

  def receive = {
    case Import(url) =>
      context.become(importing)
      importDataSet(url)
  }
}

trait ImportService extends NodeFunctions with Logger {

  implicit def actorRef: ActorRef
  implicit def actorRefFactory: ActorRefFactory

  val masterActorRef = actorRefFactory.actorOf(Props(new Master(actorRef)), "master")
  val nodeServiceActorRef = actorRefFactory.actorOf(Props(new NodeServiceActor(masterActorRef.path))
    .withRouter(RoundRobinRouter(nrOfInstances = 2)), "node-service")

  def importDataSet(url: String) = {

    val data        = XML.load(url)
    val ds: DataSet = data
    val nodes       = data \ "nodes" \ "node"
    val dn          = nodes map { DataNode.fromNode(_, ds.name) }

    Database.forURL(Configuration.databaseURL, driver = Configuration.databaseDriver) withSession {

      // open changeset
      OpenStreetMap.openChangeset(ds.tags)
      // mark all the nodes for delete.
      updateAllMarkDelete(ds.name)
      // save all the nodes.
      dn foreach { n =>

        val node = DataNodes.findOneByNodeIdAndDataSet(n.nodeId, n.dataSet) match {
          case Some(existingNode) if ! n.equalsNode(existingNode) =>
            n.copy(id = existingNode.id, osmId = existingNode.osmId, version = existingNode.version, markUpdate = true, markDelete = false)
          case Some(existingNode) => existingNode.copy(markDelete = false)
          case None => n.copy(id = Some(DataNodes.create(n)))
        }

        node.osmId match {
          case Some(osmId) if node.markUpdate && isSameVersion(node) =>
            masterActorRef ! NodeServiceProtocol.Update(node) // updateOnOSM(node)
          case Some(osmId) if !node.markUpdate =>
            DataNodes.update(node)
          case Some(osmId) =>
            logger.error("Node " + osmId + " will not be updated because OSM has a newer version.")
          case None =>
            masterActorRef ! NodeServiceProtocol.Create(node) // createOnOSM(node)
        }
      }
      // delete all the nodes mark for delete.
      DataNodes.filter { n => (n.osmId isNotNull) && (n.markDelete is true) && (n.deleted is false) }
        .map { n => n }
        .foreach { n =>
          if (OpenStreetMap.getNodeVersion(n.osmId.get) == n.version.get)
            masterActorRef ! NodeServiceProtocol.Delete(n)
        }
    }

    masterActorRef ! AllWorkSent

  }

  def closeChangeset() = {
    OpenStreetMap.closeChangeset
    logger.info("changeset closed")
  }


}
