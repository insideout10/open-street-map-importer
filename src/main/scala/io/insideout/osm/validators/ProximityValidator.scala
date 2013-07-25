/**
 * @author David Riccitelli
 */
package io.insideout.osm.validators

import io.insideout.osm.models.DataNode
import scala.xml.{NodeSeq, Node, XML}
import scala.collection.mutable.StringBuilder

/**
 *
 * @constructor
 */
class ProximityValidator extends Validator {

  val apiURL = "http://overpass-api.de/api/interpreter?data=%5Btimeout%3A86400%5D%3B%20node%5Bamenity%3D%22charging_station%22%5D%28around%3A{{radius}}%2C{{latitude}}%2C{{longitude}}%29-%3E.b%3B%20.b%20out%20meta%3B"
  val radius = 100

  def validate(node: DataNode): Option[ValidationError] = {
    findNode(node) match {
      case Some(message) => Some(ValidationError("Proximity Validator", message))
      case _ => None
    }
  }

  def findNode(dn: DataNode): Option[String] = {

    val url = apiURL
      .replace("{{radius}}", radius.toString)
      .replace("{{latitude}}", dn.latitude.get.toString)
      .replace("{{longitude}}", dn.longitude.get.toString)

    val xml = XML.load(url)
    val nodes = (xml \ "node").filter { n =>
      (!dn.osmId.isDefined) || ((n \ "@id").text.toLong != dn.osmId.get)
    }

    if (0 == nodes.length)
      None
    else
      Some(getMessage(nodes, dn))

  }

  def getMessage(nodes: NodeSeq, dn: DataNode): String = {
    val message = new StringBuilder(
      "Source node %s (%f, %f) conflicts with the following %d node(s):\n"
        .format(dn.nodeId, dn.latitude.get, dn.longitude.get, nodes.length)
    )

    nodes.foreach { message append printNode(dn, _) }

    message.toString
  }


  def printNode(dn: DataNode, n: Node): String = {
    val osmId = (n \ "@id").text.toLong
    val latitude = (n \ "@lat").text.toDouble
    val longitude = (n \ "@lon").text.toDouble

    val distance = Position(latitude, longitude).distance(Position(dn.latitude.get, dn.longitude.get)) * 1000

    " * node %d (%f, %f) - distance: %f m.\n".format(osmId, latitude, longitude, distance)
  }

  case class Position(lat:BigDecimal, lng:BigDecimal){
    def distance(other:Position)={
      def toRad(number:BigDecimal)={
        number * Math.PI / 180
      }
      // default 4 sig figs reflects typical 0.3% accuracy of spherical model

      val R = 6371; //earth radius in km
      val lat1 = toRad(lat)
      val lon1 = toRad(lng)
      val lat2 = toRad(other.lat)
      val lon2 = toRad(other.lng)
      val dLat= (lat2 - lat1).toDouble
      val dLon = (lon2 - lon1).toDouble

      val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1.toDouble) * Math.cos(lat2.toDouble) *
          Math.sin(dLon / 2) * Math.sin(dLon / 2)

      val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

      val d = R * c
      BigDecimal(d)
    }
  }
}

object ProximityValidator {
  def apply() = new ProximityValidator()
}
