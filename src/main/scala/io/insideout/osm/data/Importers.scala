/**
 * @author David Riccitelli
 */
package io.insideout.osm.data

import scala.slick.driver.MySQLDriver.simple._
import scala.Predef._
import io.insideout.osm.models.Importer

object Importers extends Table[Importer]("importers") {
  def id   = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def url  = column[String]("url", O.DBType("varchar(1024)"))

  def * = id.? ~ name ~ url <> (Importer.apply _, Importer.unapply _)

  def findOneByName(name: String)(implicit session: Session): Option[Importer] = {
    Importers.filter { _.name is name }
      .map { i => i }
      .firstOption
  }
}