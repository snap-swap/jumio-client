package com.snapswap.jumio

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.Materializer
import com.snapswap.jumio.unmarshaller._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}


trait RetrievalClient {
  def scanStatus(scanReference: String): Future[JumioScanStatus]

  def scanDetails(scanReference: String): Future[JumioScan]

  def scanMdDetails(scanReference: String): Future[JumioScan]

  def scanImages(scanReference: String): Future[JumioImagesInfo]

  def mdScanImages(scanReference: String): Future[JumioImagesInfo]

  def obtainImage(href: String): Future[RawImage]

  def obtainMdImage(href: String): Future[RawImage]
}


class AkkaHttpRetrievalClient(override val clientToken: String,
                              override val clientSecret: String,
                              override val clientCompanyName: String,
                              override val clientApplicationName: String,
                              override val clientVersion: String,
                              apiHost: String = "lon.netverify.com",
                              maxRetries: Int = 10)
                             (implicit val system: ActorSystem,
                              val ctx: ExecutionContext,
                              val materializer: Materializer) extends RetrievalClient with JumioClient {

  override val log = Logging(system, this.getClass)
  private val baseURL = s"/api/netverify/v2"

  private val flow: Connection = Http().cachedHostConnectionPoolHttps[UUID](apiHost, 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio")

  private val mdFlow: Connection = Http().cachedHostConnectionPoolHttps[UUID](s"retrieval.$apiHost", 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio multi document retrieval")

  private def requestForImage(request: HttpRequest, connection: Connection): Future[RawImage] =
    send(request.withHeaders(authHeaders), connection) { r =>
      Future.successful {
        RawImage(
          r.dataBytes,
          r.contentType
        )
      }
    }


  private def parameters[T](query: Map[String, String]): String =
    if (query.isEmpty) {
      ""
    } else {
      "?" + query.map(tup => s"${tup._1}=${tup._2}").mkString("&")
    }

  private def get[T](path: String, connection: Connection, query: Map[String, String] = Map())
                    (parser: JsValue => T): Future[T] = {
    val url = baseURL + path + parameters(query)
    requestForJson(Get(url), connection)(parser)
  }

  override def scanStatus(scanReference: String): Future[JumioScanStatus] = {
    get(s"/scans/$scanReference", flow) { response =>
      response.convertTo[JumioScanStatus]
    }
  }

  override def scanDetails(scanReference: String): Future[JumioScan] = {
    get(s"/scans/$scanReference/data", flow) { response =>
      response.convertTo[JumioScan]
    }
  }

  override def scanMdDetails(scanReference: String): Future[JumioScan] = {
    get(s"/documents/$scanReference/data", mdFlow) { response =>
      response.convertTo[JumioScan]
    }
  }

  override def scanImages(scanReference: String): Future[JumioImagesInfo] =
    get(s"/scans/$scanReference/images", flow) { response =>
      response.convertTo[JumioImagesInfo]
    }

  override def mdScanImages(scanReference: String): Future[JumioImagesInfo] =
    get(s"/documents/$scanReference/pages", mdFlow) { response =>
      response.convertTo[JumioImagesInfo]
    }

  override def obtainImage(href: String): Future[RawImage] =
    requestForImage(Get(href), flow)

  override def obtainMdImage(href: String): Future[RawImage] =
    requestForImage(Get(href), mdFlow)

}