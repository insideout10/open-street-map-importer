/**
 * @author David Riccitelli
 */
package io.insideout.osm

import com.typesafe.config.ConfigFactory

object Configuration {

  val config          = ConfigFactory.load()

  def databaseURL     = config.getString("app.database.url")
  def databaseDriver  = "com.mysql.jdbc.Driver" // config.getString("app.database.driver")

  object OpenStreetMap {
    def apiURL        = config.getString("app.open-street-map.api.url")
    def username      = config.getString("app.open-street-map.api.username")
    def password      = config.getString("app.open-street-map.api.password")
    def maxThreads    = config.getInt("app.open-street-map.api.max-threads")
  }

  object Overpass {
    def apiURL        = config.getString("app.overpass.url")
    def parameters    = config.getString("app.overpass.parameters")
    def radius        = config.getInt("app.overpass.radius")
  }

}
