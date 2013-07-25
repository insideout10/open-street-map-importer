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

  /**
   * Create or update the node in the database, and cancel the mark delete flag.
   *
   * @param node The node to update or create.
   * @return The created or updated node.
   */
  def mergeNode(node: DataNode): DataNode = {

    // look for an existing node and merge data if any.
    val n: DataNode = DataNodes.findOneByNodeIdAndDataSet(node.nodeId, node.dataSet) match {

      case Some(existingNode) =>
        node.copy(
          id = existingNode.id, osmId = existingNode.osmId, version = existingNode.version,
          markUpdate = !node.equalsNode(existingNode),
          markDelete = false)

      case _ => node.copy(markDelete = false)

    }

    // create or update the node. cancel the delete flag.
    DataNodes.save(n)

  }

  def syncOSM(node: DataNode): Unit = node.osmId match {
    case None =>
      masterActorRef ! NodeServiceProtocol.Create(node) // createOnOSM(node)
    case _ if node.markUpdate =>
      masterActorRef ! NodeServiceProtocol.Update(node) // updateOnOSM(node)
    case _ => //
  }

  def syncScore(node: DataNode): Int = {
    if (isSameVersion(node))
      0
    else
      -1
  }

  def validateNode(node: DataNode): Boolean = {
    val sameVersion = isSameVersion(node)
    if (!sameVersion) logger.error("Node is not at the same version")
    sameVersion
  }

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

        // create the node in the database if it doesn't exist yet, and load existing OSM data if any.
        val node = mergeNode(n)

        if (0 == syncScore(node)) syncOSM(node)
//        if (validateNode(node)) syncOSM(node) // synchronize OSM data

      }
      // delete all the nodes mark for delete.
      DataNodes.filter { n => (n.osmId isNotNull) && (n.markDelete is true) && (n.deleted is false) }
        .map { n => n }
        .foreach { n =>
          if (OpenStreetMap.getNodeVersion(n.osmId.get) == n.version)
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
