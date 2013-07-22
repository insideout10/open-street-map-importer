/**
 * @author David Riccitelli
 */
package io.insideout.osm.data

import scala.slick.driver.MySQLDriver.simple._
import io.insideout.osm.Configuration

object DataAccess {

  def database = Database.forURL(Configuration.databaseURL, driver = Configuration.databaseDriver)

}
