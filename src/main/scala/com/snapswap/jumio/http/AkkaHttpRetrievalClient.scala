package com.snapswap.jumio.http


import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.Materializer
import com.snapswap.jumio._
import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import com.snapswap.jumio.model.retrieval.{JumioImageRawData, JumioImagesInfo, JumioScan, JumioScanStatus}
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
                              val materializer: Materializer) extends RetrievalClient with JumioHttpClient {

  override val log = Logging(system, this.getClass)
  private val baseURL = s"/api/netverify/v2"

  private lazy val flow: Connection = Http().cachedHostConnectionPoolHttps[UUID](apiHost, 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio")

  private lazy val mdFlow: Connection = Http().cachedHostConnectionPoolHttps[UUID](s"retrieval.$apiHost", 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio multi document retrieval")

  private def requestForImage(request: HttpRequest, isMd: Boolean): Future[JumioImageRawData] = {
    send(request.withHeaders(authHeaders), if (isMd) mdFlow else flow)(parseRawImage)
  }

  private def parseRawImage: ResponseEntity => Future[JumioImageRawData] = { response =>
    Future.successful {
      JumioImageRawData(
        response.dataBytes,
        response.contentType
      )
    }
  }

  private def parameters[T](query: Map[String, String]): String =
    if (query.isEmpty) {
      ""
    } else {
      "?" + query.map(tup => s"${tup._1}=${tup._2}").mkString("&")
    }


  private def get[T](path: String, isMd: Boolean, query: Map[String, String] = Map())
                    (parser: JsValue => T): Future[T] = {
    val url = baseURL + path + parameters(query)
    requestForJson(Get(url), if (isMd) mdFlow else flow)(parser)
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

  override def obtainImage(href: String): Future[JumioImageRawData] =
    requestForImage(Get(href), isMd = false)

  override def obtainMdImage(href: String): Future[JumioImageRawData] =
    requestForImage(Get(href), isMd = true)

}
