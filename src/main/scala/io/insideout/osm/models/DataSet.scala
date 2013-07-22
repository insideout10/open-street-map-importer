/**
 * @author David Riccitelli
 */
package io.insideout.osm.models

import scala.xml.Node

case class DataSet(val name: String, val email: String, val overwriteNewVersions: Boolean, val tags: String)

object DataSet {

  implicit def nodeToDataSet(n: Node): DataSet = DataSet(
    (n \ "@name").text,
    (n \ "@email").text,
    ("yes" == (n \ "@overwrite-new-versions").text),
    (n \ "tags" \ "tag").toString
  )
}
