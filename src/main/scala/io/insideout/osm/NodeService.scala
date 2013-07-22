/**
 * @author David Riccitelli
 */
package io.insideout.osm

import akka.actor.{ActorRef, ActorPath, ActorLogging}
import io.insideout.osm.models.DataNode
import io.insideout.osm.data.{DataNodes, DataAccess}
import scala.slick.session.Session
import io.insideout.osm.masterworker.Worker
import scala.concurrent.Future
import akka.pattern.pipe

object NodeServiceProtocol {
  case class Create(node: DataNode)
  case class Update(node: DataNode)
  case class Delete(node: DataNode)
}

class NodeServiceActor(master: ActorPath) extends Worker(master) with ActorLogging with NodeService {
  import NodeServiceProtocol._

  implicit val ec = context.dispatcher

  def doWork(workSender: ActorRef, work: Any) = {
    Future {
      work match {
        case Create(node) => create(node)
        case Update(node) => update(node)
        case Delete(node) => delete(node)
      }
      WorkComplete("done")
    } pipeTo self
  }
}

trait NodeService extends NodeFunctions with Logger {

  def create(node: DataNode) = DataAccess.database.withSession { implicit session: Session =>
    logger.info("creating node [ id :: %d ][ nodeId :: %s ]".format(node.id.get, node.nodeId))
    DataNodes.update(createOnOSM(node))
  }
  def update(node: DataNode) = DataAccess.database.withSession { implicit session: Session =>
    logger.info(
      "updating node [ id :: %d ][ nodeId :: %s ][ osmId :: %d ][ version :: %d ]".format(
      node.id.get, node.nodeId, node.osmId.get, node.version.get))
    DataNodes.update(updateOnOSM(node))
  }
  def delete(node: DataNode) = DataAccess.database.withSession { implicit session: Session =>
    logger.info("deleting node [ id :: %d ][ nodeId :: %s ][ osmId :: %d ][ version :: %d ]".format(
      node.id.get, node.nodeId, node.osmId.get, node.version.get))
    DataNodes.update(deleteOnOSM(node))
  }

}
