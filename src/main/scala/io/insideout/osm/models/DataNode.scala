/**
 * @author David Riccitelli
 */
package io.insideout.osm.models

import scala.xml.{XML, Node}
import scalaz.Scalaz._
import io.insideout.osm.Logger

/**
 *
 * @constructor
 */
case class DataNode(val id: Option[Long], val nodeId: String,
                    val latitude: Option[Double],val longitude: Option[Double],
                    val osmId: Option[Long], val version: Option[Long], val tags: String, val dataSet: String,
                    val markDelete: Boolean, val deleted: Boolean, val markUpdate: Boolean) {

  def equalsNode(node: DataNode) = (nodeId == node.nodeId) && (tags == node.tags)
}

object DataNode extends Logger {

  def fromNode(n: Node, ds: String) = {

    val nodeId                    = (n \ "@id").text
    val latitude: Option[Double]  = (n \ "@lat").text.parseDouble.toOption
    val longitude: Option[Double] = (n \ "@lon").text.parseDouble.toOption
    val tags: String              = (n \ "tag").toString

    // create the datanode
    DataNode(None, nodeId, latitude, longitude, None, None, tags, ds, false, false, false)
  }
}
