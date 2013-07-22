/**
 * @author David Riccitelli
 */
package io.insideout.osm.data

import scala.slick.driver.MySQLDriver.simple._
import scala.Predef._
import io.insideout.osm.models.DataNode

object DataNodes extends Table[DataNode]("nodes") {
  def id        = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def nodeId     = column[String]("node_id")
  def latitude   = column[Double]("latitude", O.Nullable)
  def longitude  = column[Double]("longitude", O.Nullable)
  def osmId      = column[Long]("osm_id", O.Nullable)
  def version    = column[Long]("version", O.Nullable)
  def tags       = column[String]("tags", O.Nullable, O.DBType("text"))
  def dataSet    = column[String]("data_set")
  def markDelete = column[Boolean]("mark_delete", O.Default(false))
  def deleted    = column[Boolean]("deleted", O.Default(false))
  def markUpdate = column[Boolean]("mark_update", O.Default(false))

  def * = id.? ~ nodeId ~ latitude.? ~ longitude.? ~ osmId.? ~ version.? ~ tags ~ dataSet ~ markDelete ~ deleted ~ markUpdate <>
    (DataNode.apply _, DataNode.unapply _)

  def forInsert = nodeId ~ latitude.? ~ longitude.? ~ osmId.? ~ version.? ~ tags ~ dataSet ~ markDelete ~ deleted ~ markUpdate <>
    (
      { t => DataNode(None, t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10)},
      { (n: DataNode) => Some((n.nodeId, n.latitude, n.longitude, n.osmId, n.version, n.tags, n.dataSet, n.markDelete, n.deleted, n.markUpdate))}
    )

  def findOneByNodeIdAndDataSet(nodeId: String, dataSet: String)(implicit session: Session): Option[DataNode] =
    DataNodes.filter { n => (n.nodeId is nodeId) && (n.dataSet is dataSet) }
      .map { n => n }
      .firstOption

  def update(node: DataNode)(implicit session: Session) =
    DataNodes.filter { _.id is node.id }
      .update(node)

  def create(n: DataNode)(implicit session: Session): Long =
    forInsert returning id insert(n)

}