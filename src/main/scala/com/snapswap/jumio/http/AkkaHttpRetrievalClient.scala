package com.snapswap.jumio.http


import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.client.RequestBuilding._
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import com.snapswap.http.client.HttpClient
import com.snapswap.http.client.HttpConnection._
import com.snapswap.jumio._
import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import com.snapswap.jumio.model.retrieval._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}


class AkkaHttpRetrievalClient(override val clientToken: String,
                              override val clientSecret: String,
                              override val clientCompanyName: String,
                              override val clientApplicationName: String,
                              override val clientVersion: String,
                              apiHost: String = "lon.netverify.com",
                              maxRetries: Int = 10)
                             (implicit val system: ActorSystem,
                              val ctx: ExecutionContext,
                              val materializer: Materializer)
  extends RetrievalClient
    with JumioHttpClient
    with JumioHttpImageClient {

  override val log = Logging(system, this.getClass)
  private val v3BaseURL = s"/api/netverify/v2"
  private val mdApiHost = s"retrieval.$apiHost"

  private val connection: Connection[NotUsed] = superPool().log("jumio netverify retrieval")
  private val client: HttpClient[NotUsed] = HttpClient(connection, 5000, OverflowStrategy.dropNew)


  private def parameters[T](query: Map[String, String]): String =
    if (query.isEmpty) {
      ""
    } else {
      "?" + query.map(tup => s"${tup._1}=${tup._2}").mkString("&")
    }

  private def get[T](path: String, isMd: Boolean, query: Map[String, String] = Map())
                    (parser: JsValue => T): Future[T] = {
    val request = Get(v3BaseURL + path + parameters(query))
    requestForJson(request.withUri(request.uri.withHost(if (isMd) mdApiHost else apiHost).withScheme("https")), client)(parser)
  }

  override def scanStatus(scanReference: String): Future[JumioScanStatus] = {
    get(s"/scans/$scanReference", isMd = false) { response =>
      response.convertTo[JumioScanStatus]
    }
  }

  override def scanDetails(scanReference: String): Future[JumioScan] = {
    get(s"/scans/$scanReference/data", isMd = false) { response =>
      response.convertTo[JumioScan]
    }
  }

  override def scanMdDetails(scanReference: String): Future[JumioScan] = {
    get(s"/documents/$scanReference/data", isMd = true) { response =>
      response.convertTo[JumioScan]
    }
  }

  override def scanImages(scanReference: String): Future[JumioImagesInfo] =
    get(s"/scans/$scanReference/images", isMd = false) { response =>
      response.convertTo[JumioImagesInfo]
    }

  override def mdScanImages(scanReference: String): Future[JumioImagesInfo] =
    get(s"/documents/$scanReference/pages", isMd = true) { response =>
      response.convertTo[JumioImagesInfo]
    }

  override def obtainImage(images: Seq[JumioImage]): Source[(JumioImageRawData, JumioImage), Any] =
    getImages(images, apiHost, client)

  override def obtainMdImage(images: Seq[JumioImage]): Source[(JumioImageRawData, JumioImage), Any] =
    getImages(images, mdApiHost, client)

}
