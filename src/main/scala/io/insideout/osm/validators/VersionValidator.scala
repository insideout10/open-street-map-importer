/**
 * @author David Riccitelli
 */
package io.insideout.osm.validators

import io.insideout.osm.models.DataNode
import io.insideout.osm.OpenStreetMap

/**
 *
 * @constructor
 */
class VersionValidator extends Validator {

  def validate(node: DataNode): Option[ValidationError] =  {
    if (!node.osmId.isDefined)
      return None

    val osmNodeVersion = OpenStreetMap.getNodeVersion(node.osmId.get)

    if (osmNodeVersion != node.version)
      Some(
        ValidationError(
          "NodeVersion",
          "Source node %s version (v%s) is different from OpenStreetMap node version (v%s)."
            .format(
              node.nodeId,
              node.version.getOrElse("unknown").toString,
              osmNodeVersion.getOrElse("unknown").toString
            )
        )
      )
    else
      None
  }

}

object VersionValidator {
  def apply() = new VersionValidator()
}