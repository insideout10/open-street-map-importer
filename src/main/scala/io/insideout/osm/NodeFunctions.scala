/**
 * @author David Riccitelli
 */
package io.insideout.osm

import io.insideout.osm.models.DataNode
import io.insideout.osm.data.DataNodes
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

/**
 *
 * @constructor
 */
trait NodeFunctions extends Logger {

  def updateAllMarkDelete(dataSet: String) = DataNodes.filter { n => (n.dataSet is dataSet) && (n.deleted is false) }
    .map { n => n.markDelete }
    .update(true)

  def isSameVersion(node: DataNode) = OpenStreetMap.getNodeVersion(node.osmId.get) == node.version.get

  def deleteIfSameVersion(node: DataNode): Unit =
    if (isSameVersion(node)) {
      OpenStreetMap.deleteNode(node)
      DataNodes.update(node.copy(deleted = true))
    }
    else
      logger.error("Node " + node.osmId.get + " will not be delete because OSM has a newer version.")


  def createOnOSM(node: DataNode): DataNode = OpenStreetMap.createNode(node) match {
    case r: String => node.copy(osmId = Option(r.toLong), version = Option(1))
    case e         => logger.error("Something wrong happened: " + e.toString); node
  }

  def updateOnOSM(node: DataNode): DataNode = OpenStreetMap.updateNode(node) match {
    case r: String => node.copy(version = Option(r.toLong), markUpdate = false)
    case e         => logger.error("Something wrong happened: " + e.toString); node
  }

  def deleteOnOSM(node: DataNode): DataNode = {
    OpenStreetMap.deleteNode(node)
    node.copy(deleted = true)
  }

}
