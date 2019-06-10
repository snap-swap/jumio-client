package com.snapswap.jumio.http

import java.util.Base64

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import com.snapswap.http.client.HttpClient
import com.snapswap.http.client.HttpConnection._
import com.snapswap.jumio._
import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import com.snapswap.jumio.model._
import com.snapswap.jumio.model.init._
import com.snapswap.jumio.model.netverify.{AcceptedIdDocs, PerformNetverifyRequest, PerformNetverifyResponse}
import com.snapswap.jumio.model.retrieval.JumioImageRawData
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

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
  private val v3BaseURL = s"/api/netverify/v2"
  private val v4BaseURL = s"/api/v4"

  private val connection: Connection[NotUsed] = superPool().log("jumio")
  private val mdApiHost = s"upload.$apiHost"

  private val client: HttpClient[NotUsed] = HttpClient(connection, 5000, OverflowStrategy.dropNew)

  override def listAcceptedIdDocs(): Future[AcceptedIdDocs] =
    getV3("/acceptedIdTypes", isMd = false)(j => j.convertTo[AcceptedIdDocs])


  private def getV3[T](path: String, isMd: Boolean)
                      (parser: JsValue => T): Future[T] = {
    val request = Get(Uri(v3BaseURL + path).withHost(if (isMd) mdApiHost else apiHost).withScheme("https"))
    requestForJson(request, client)(parser)
  }

  private def postV3[T](path: String, data: JsValue, isMd: Boolean)(parser: JsValue => T): Future[T] = {
    val request =
      Post(Uri(v3BaseURL + path).withHost(if (isMd) mdApiHost else apiHost).withScheme("https"))
        .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`), data.compactPrint))
    requestForJson(request, client)(parser)
  }

  private def postV4[T](path: String, data: JsValue, isMd: Boolean)(parser: JsValue => T): Future[T] = {
    val request =
      Post(Uri(v4BaseURL + path).withHost(if (isMd) mdApiHost else apiHost).withScheme("https"))
        .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`), data.compactPrint))
    requestForJson(request, client)(parser)
  }

  override def initNetverifyV3(merchantScanReference: String,
                               redirectUrl: String,
                               callbackUrl: String,
                               customerId: String): Future[JumioNetverifyInitResponseV3] = {
    val params = JumioNetverifyInitParamsV3(merchantScanReference, redirectUrl, redirectUrl, callbackUrl, customerId)

    postV3("/initiateNetverify", params.toJson, isMd = false) { response =>
      response.convertTo[JumioNetverifyInitResponseV3]
    }
  }

  override def initNetverifyV4(merchantScanReference: String,
                               redirectUrl: String,
                               callbackUrl: String,
                               customerId: String): Future[JumioNetverifyInitResponseV4] = {
    val params = JumioNetverifyInitParamsV4(
      customerInternalReference = merchantScanReference,
      successUrl = redirectUrl,
      errorUrl = redirectUrl,
      callbackUrl = callbackUrl,
      userReference = customerId
    )

    postV4("/initiate", params.toJson, isMd = false) { response =>
      response.convertTo[JumioNetverifyInitResponseV4]
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
    postV3("/acquisitions", params.toJson, isMd = true) { response =>
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
      result <- postV3("/performNetverify", params.toJson, isMd = false) { response =>
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
