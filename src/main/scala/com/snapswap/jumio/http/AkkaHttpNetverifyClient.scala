package com.snapswap.jumio.http

import java.util.Base64

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import spray.json._
import com.snapswap.http.client.HttpClient
import com.snapswap.http.client.HttpConnection._
import com.snapswap.jumio._
import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import com.snapswap.jumio.model._
import com.snapswap.jumio.model.init.{JumioMdNetverifyInitParams, JumioMdNetverifyInitResponse, JumioNetverifyInitParams, JumioNetverifyInitResponse}
import com.snapswap.jumio.model.netverify.{AcceptedIdDocs, PerformNetverifyRequest, PerformNetverifyResponse}
import com.snapswap.jumio.model.retrieval.JumioImageRawData

class AkkaHttpNetverifyClient(override val clientToken: String,
                              override val clientSecret: String,
                              override val clientCompanyName: String,
                              override val clientApplicationName: String,
                              override val clientVersion: String,
                              apiHost: String = "lon.netverify.com",
                              maxRetries: Int = 10)
                             (implicit val system: ActorSystem,
                              val ctx: ExecutionContext,
                              val materializer: Materializer) extends NetverifyClient with JumioHttpClient {

  override val log = Logging(system, this.getClass)
  private val baseURL = s"/api/netverify/v2"

  //FIXME: Rewrite to use superPool HERE
  private val connection: Connection[HostConnectionPool] = httpsPool(apiHost, 443,
    defaultClientHttpsContext,
    defaultConnectionPoolSettings.withMaxRetries(maxRetries),
    systemLogging
  ).log("jumio")

  //FIXME: Rewrite to use superPool HERE
  private val mdConnection: Connection[HostConnectionPool] = httpsPool(s"upload.$apiHost", 443,
    defaultClientHttpsContext,
    defaultConnectionPoolSettings.withMaxRetries(maxRetries),
    systemLogging
  ).log("jumio multi document")

  private val client: HttpClient[HostConnectionPool] = HttpClient(connection, 5000, OverflowStrategy.dropNew)
  private val mdClient: HttpClient[HostConnectionPool] = HttpClient(mdConnection, 5000, OverflowStrategy.dropNew)


  override def listAcceptedIdDocs(): Future[AcceptedIdDocs] =
    get("/acceptedIdTypes", isMd = false)(j => j.convertTo[AcceptedIdDocs])


  private def get[T](path: String, isMd: Boolean)
                    (parser: JsValue => T): Future[T] = {
    val url = baseURL + path
    requestForJson(Get(url), if (isMd) mdClient else client)(parser)
  }

  private def post[T](path: String, data: JsValue, isMd: Boolean)(parser: JsValue => T): Future[T] = {
    val request =
      Post(baseURL + path)
        .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`), data.toString))

    requestForJson(request, if (isMd) mdClient else client)(parser)
  }

  override def initNetverify(merchantScanReference: String,
                             redirectUrl: String,
                             callbackUrl: String,
                             customerId: String): Future[JumioNetverifyInitResponse] = {
    val params = JumioNetverifyInitParams(merchantScanReference, redirectUrl, redirectUrl, callbackUrl, customerId)

    post("/initiateNetverify", params.toJson, isMd = false) { response =>
      response.convertTo[JumioNetverifyInitResponse]
    }
  }

  override def initMdNetverify(merchantScanReference: String,
                               redirectUrl: String,
                               callbackUrl: String,
                               customerId: String,
                               country: String,
                               docType: EnumJumioDocTypes.JumioDocType): Future[JumioMdNetverifyInitResponse] = {
    val params = JumioMdNetverifyInitParams(
      merchantScanReference, redirectUrl, redirectUrl, callbackUrl, customerId, country, docType
    )
    post("/acquisitions", params.toJson, isMd = true) { response =>
      response.convertTo[JumioMdNetverifyInitResponse]
    }
  }

  override def performNetverify(merchantIdScanReference: String,
                                country: String,
                                face: Option[JumioImageRawData],
                                idType: EnumJumioDocTypes.JumioDocType,
                                idFront: JumioImageRawData,
                                idBack: Option[JumioImageRawData],
                                callbackUrl: String,
                                customerId: Option[String],
                                clientIp: Option[String]): Future[PerformNetverifyResponse] = {
    for {
      faceString: Option[String] <- face.map(f => {
        encode(f.data).map(Some(_))
      }).getOrElse {
        Future.successful(None)
      }
      idFrontString <- encode(idFront.data)
      idBackString: Option[String] <- idBack.map(_.data).map(encode).map(_.map(Some(_))).getOrElse(Future.successful(None))
      params = PerformNetverifyRequest(
        merchantIdScanReference = merchantIdScanReference,
        faceImage = faceString,
        faceImageMimeType = face.map(_.contentType.mediaType.toString),
        frontsideImage = idFrontString,
        frontsideImageMimeType = idFront.contentType.mediaType.toString,
        backsideImage = idBackString,
        backsideImageMimeType = idBack.map(_.contentType.mediaType.toString),
        country = country,
        idType = idType.toString,
        callbackUrl = callbackUrl,
        enabledFields = face match {
          case Some(_) =>
            "idNumber,idFirstName,idLastName,idDob,idExpiry,idUsState,idPersonalNumber,idFaceMatch,idAddress"
          case None =>
            "idNumber,idFirstName,idLastName,idDob,idExpiry,idUsState,idPersonalNumber,idAddress"
        },
        customerId = customerId,
        clientIp = clientIp
      )
      _ = log.debug(s"Use 'performNetverify' with parameters: $params")
      result <- post("/performNetverify", params.toJson, isMd = false) { response =>
        response.convertTo[PerformNetverifyResponse]
      }
    } yield result
  }

  private def encode(data: Source[ByteString, Any]): Future[String] = {
    data.runFold(ByteString.createBuilder) { case (builder, bs) =>
      builder.append(bs)
    }.map(builder => Base64.getEncoder.encodeToString(builder.result().toArray))
  }
}
