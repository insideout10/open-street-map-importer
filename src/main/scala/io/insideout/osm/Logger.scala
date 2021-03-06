/**
 * @author David Riccitelli
 */
package io.insideout.osm

import org.slf4j.LoggerFactory

trait Logger {
  val logger = LoggerFactory.getLogger(getClass)
}
