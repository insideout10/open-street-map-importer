/**
 * @author David Riccitelli
 */
package io.insideout.osm.masterworker

import akka.actor.ActorRef

object MasterWorkerProtocol {
  // Messages from Workers
  case class WorkerCreated(worker: ActorRef)
  case class WorkerRequestsWork(worker: ActorRef)
  case class WorkIsDone(worker: ActorRef)

  // Messages to Workers
  case class WorkToBeDone(work: Any)
  case object WorkIsReady
  case object NoWorkToBeDone

  // Message to Master
  case object AllWorkSent

  // Message to ResultHandler
  case object AllWorkCompleted
}