/**
 * @author David Riccitelli
 */
package io.insideout.osm

import org.apache.http.client.fluent.Async
import org.apache.http.entity.StringEntity
import java.util.concurrent.Executors
import java.net.{URL, URI}
import org.apache.http.impl.client.{BasicAuthCache, DefaultHttpClient}
import org.apache.http.auth.{UsernamePasswordCredentials, AuthScope}
import org.apache.http.client.AuthCache
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.client.protocol.ClientContext
import org.apache.http.{HttpEntity, HttpResponse, HttpHost}
import org.apache.http.client.methods.{HttpGet, HttpPut, HttpRequestBase}
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import scala.xml.XML
import io.insideout.osm.models.DataNode

sealed abstract class HttpRequest
case class Put(path: String, body: String)    extends HttpRequest
case class Delete(path: String, body: String) extends HttpRequest
case class Get(path: String)                  extends HttpRequest

object OpenStreetMap {
  val log         = LoggerFactory.getLogger(getClass)

  val api         = Configuration.OpenStreetMap.apiURL
  val username    = Configuration.OpenStreetMap.username
  val password    = Configuration.OpenStreetMap.password

  val threadpool  = Executors.newFixedThreadPool(Configuration.OpenStreetMap.maxThreads)
  val async       = Async.newInstance().use(threadpool)


  var sessions    = 0
  var changesetId = ""

  def openChangeset(tags: String = "") = {
    sessions += 1
    synchronized {
      if (changesetId.isEmpty) {

        val body =
          """|<osm>
             | <changeset>
             |  %s
             | </changeset>
             |</osm>""".stripMargin.format(tags)

        changesetId = request(Put("/changeset/create", body)).toString

        log.debug(s"Changset open [ changesetId :: ${changesetId} ]")
      }
    }
  }

  def closeChangeset = {
    sessions -= 1
    synchronized {
      if (0 == sessions && !changesetId.isEmpty) {
        request(Put("/changeset/" + changesetId + "/close", body = ""))
        log.debug(s"Changeset close [ changesetId :: ${changesetId} ]")
        changesetId = ""
      }
    }
  }

  def createNode(n: DataNode) = {

    val body =
      """|<osm>
         | <node visible="true" changeset="%s" lat="%f" lon="%f">
         |  %s
         | </node>
         |</osm>""".stripMargin.format(changesetId, n.latitude.get, n.longitude.get, n.tags)

    request(Put("/node/create", body))
  }

  def updateNode(n: DataNode) = {

    val body =
      """|<osm>
         | <node id="%d" version="%d" visible="true" changeset="%s" lat="%f" lon="%f">
         |   %s
         | </node>
         |</osm>""".stripMargin.format(n.osmId.get, n.version.get, changesetId, n.latitude.get, n.longitude.get, n.tags)

    request(Put(s"/node/${n.osmId.get}", body))
  }

  def deleteNode(n: DataNode) = {
    val body =
      """|<osm>
         | <node id="%d" version="%d" visible="true" changeset="%s" lat="%f" lon="%f" />
         |</osm>""".stripMargin.format(n.osmId.get, n.version.get, changesetId, n.latitude.get, n.longitude.get)

    request(Delete(s"/node/${n.osmId.get}", body))
  }

  /**
   * Get a node from OpenStreetMap given its __node id__.
   *
   * @param nodeId The node id.
   * @return A string with the XML representation of the node.
   */
  def getNode(nodeId: Long) = request(Get("/node/" + nodeId.toString)).toString

  /**
   * Get the version of the node on OSM. If the node does not exist (or it has been deleted), returns None.
   * @param nodeId The node id.
   * @return The node version or None.
   */
  def getNodeVersion(nodeId: Long): Option[Long] =
    try {
      val version = (XML.loadString(getNode(nodeId)) \ "node" \ "@version").text
      Some(version.toLong)
    } catch {
      case _: Throwable => None
    }

  private def request(r: HttpRequest) = r match {
    case Get(path)          => internalRequest(api + path, "GET")
    case Put(path, body)    => internalRequest(api + path, "PUT", body)
    case Delete(path, body) => internalRequest(api + path, "DELETE", body)
    case _                  => log.warn("**** don't know what to do with this http request.")
  }


  private def internalRequest(url: String, method: String, body: String = ""): String = {

    // basic authentication
    val u: URL = new URL(url)
    val client: DefaultHttpClient = new DefaultHttpClient()

    client.getCredentialsProvider().setCredentials(
      new AuthScope(u.getHost(), u.getPort()),
      new UsernamePasswordCredentials(username, password))

    // Create AuthCache instance
    val authCache: AuthCache = new BasicAuthCache()

    // Generate BASIC scheme object and add it to the local
    // auth cache
    val basicAuth: BasicScheme = new BasicScheme()

    authCache.put(new HttpHost(u.getHost(), u.getPort(), u.getProtocol()),
      basicAuth)

    // Add AuthCache to the execution context
    val context: BasicHttpContext = new BasicHttpContext()
    context.setAttribute(ClientContext.AUTH_CACHE, authCache)

    var request: HttpRequestBase = null

    method match {
      case "PUT"    => {
        request = new HttpPut(url);
        (request.asInstanceOf[HttpPut]).setEntity(new StringEntity(body, "utf-8"));
      }

      case "GET"    => request = new HttpGet(url);

      case "DELETE" => {
        request = new HttpDeleteWithEntity(URI.create(url))
        (request.asInstanceOf[HttpDeleteWithEntity]).setEntity(new StringEntity(body, "utf-8"))
      }

      case _        => return ""
    }

    val response: HttpResponse = client.execute(request, context)

    var responseBody: String   = ""
    try {
      val  entity: HttpEntity = response.getEntity();
      responseBody = EntityUtils.toString(entity, "utf-8");
      EntityUtils.consume(entity);
    } finally {
      request.releaseConnection();
    }

    if (200 != response.getStatusLine().getStatusCode())
      log.debug(
        s"**** [ statusLine :: ${response.getStatusLine()} ][ method :: $method ][ url :: $url ][ responseBody :: $responseBody ]")

    //    println(s"**** sending request [ method :: $method ][ url :: $url ][ body :: $body ][ responseBody :: $responseBody ]")

    responseBody
  }
}
