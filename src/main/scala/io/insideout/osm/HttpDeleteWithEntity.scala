/**
 * @author David Riccitelli
 */

package io.insideout.osm

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import java.net.URI

/**
 *
 * @constructor
 */
class HttpDeleteWithEntity(uri: URI) extends HttpEntityEnclosingRequestBase() {
  setURI(uri)

  override def getMethod: String = "DELETE"
}