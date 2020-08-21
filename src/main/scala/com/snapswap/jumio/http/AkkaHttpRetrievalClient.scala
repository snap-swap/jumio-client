package com.snapswap.jumio.http


import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.client.RequestBuilding._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.snapswap.http.client.HttpClient
import com.snapswap.jumio._
import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import com.snapswap.jumio.model.retrieval._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}


class AkkaHttpRetrievalClient(override val clientCompanyName: String,
                              override val clientApplicationName: String,
                              override val clientVersion: String,
                              maxRetries: Int = 10)
                             (implicit val system: ActorSystem,
                              val ctx: ExecutionContext,
                              val materializer: Materializer)
  extends RetrievalClient
    with JumioHttpClient
    with JumioHttpImageClient {

  override val log = Logging(system, this.getClass)
  private val v3BaseURL = s"/api/netverify/v2"

  private val client: HttpClient = HttpClient(logger = Some(log))


  private def parameters[T](query: Map[String, String]): String =
    if (query.isEmpty) {
      ""
    } else {
      "?" + query.map(tup => s"${tup._1}=${tup._2}").mkString("&")
    }

  private def get[T](path: String, query: Map[String, String] = Map())
                    (parser: JsValue => T)
                    (implicit params: JumioRetrievalConnectionParams): Future[T] = {
    val request = Get(v3BaseURL + path + parameters(query))
    requestForJson(request.withUri(params.apiHost + request.uri), client)(parser)
  }

  override def scanStatus(scanReference: String)
                         (implicit params: JumioRetrievalConnectionParams): Future[JumioScanStatus] = {
    get(s"/scans/$scanReference") { response =>
      response.convertTo[JumioScanStatus]
    }
  }

  override def scanDetails(scanReference: String)
                          (implicit params: JumioRetrievalConnectionParams): Future[JumioScan] = {
    get(s"/scans/$scanReference/data") { response =>
      response.convertTo[JumioScan]
    }
  }

  override def scanImages(scanReference: String)
                         (implicit params: JumioRetrievalConnectionParams): Future[JumioImagesInfo] =
    get(s"/scans/$scanReference/images") { response =>
      response.convertTo[JumioImagesInfo]
    }

  override def obtainImage(images: Seq[JumioImage])
                          (implicit params: JumioRetrievalConnectionParams): Source[(JumioImageRawData, JumioImage), Any] =
    getImages(images, client)

}
