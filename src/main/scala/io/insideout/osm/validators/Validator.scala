/**
 * @author David Riccitelli
 */
package io.insideout.osm.validators

import io.insideout.osm.models.{DataSet, DataNode}

case class ValidationError(source: String, message: String)

/**
 *
 * @constructor
 */
abstract class Validator {
  def validate(node: DataNode): Option[ValidationError]
}
