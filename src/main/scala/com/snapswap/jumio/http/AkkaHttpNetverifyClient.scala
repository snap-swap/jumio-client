package com.snapswap.jumio.http


import java.util.{Base64, UUID}

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.{ByteString, Timeout}
import com.snapswap.jumio._
import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import com.snapswap.jumio.model._
import com.snapswap.jumio.model.errors.JumioEntityNotFoundError
import com.snapswap.jumio.model.init.{JumioMdNetverifyInitParams, JumioMdNetverifyInitResponse, JumioNetverifyInitParams, JumioNetverifyInitResponse}
import com.snapswap.jumio.model.netverify.{PerformNetverifyRequest, PerformNetverifyResponse}
import com.snapswap.jumio.model.retrieval.JumioImageRawData
import spray.json._

import scala.concurrent.duration._
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
  private val baseURL = s"/api/netverify/v2"

  private lazy val flow: Connection = Http().cachedHostConnectionPoolHttps[UUID](apiHost, 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio")

  private lazy val mdFlow: Connection = Http().cachedHostConnectionPoolHttps[UUID](s"upload.$apiHost", 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio multi document")


  ///////////////////////// EXPERIMENTAL CLIENT /////////////////////////
  private implicit val timeout: Timeout = Timeout(1.minute)

  private lazy val streamingFlow: HttpClient.StreamingConnection =
    Http().cachedHostConnectionPoolHttps[HttpClient.ProxyMeta](apiHost, 443,
      settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio streaming")

  private lazy val experimentalHttpClient: ActorRef = HttpClient.apply(streamingFlow, 5000, OverflowStrategy.dropNew)

  private lazy val httpProxy = system.actorOf(HttpClientProxy.props(experimentalHttpClient))

  private def processStreamingResponse(response: HttpClient.WrappedResponse): HttpResponse = response match {
    case HttpClient.WrappedResponse(Left(ex), HttpClient.ProxyMeta(_, _, req)) =>
      log.error(ex, s"Response to ${req.method.value} ${req.uri} failed")
      throw ex
    case HttpClient.WrappedResponse(Right(r), HttpClient.ProxyMeta(_, _, req)) if r.status == StatusCodes.NotFound =>
      log.error(s"Response to ${req.method.value} ${req.uri} is ${r.status}")
      r.discardEntityBytes()
      throw JumioEntityNotFoundError
    case HttpClient.WrappedResponse(Right(r), HttpClient.ProxyMeta(_, _, req)) =>
      log.info(s"Response to ${req.method.value} ${req.uri} is ${r.status}")
      r
  }

  ////////////////////////////////////////////////////////////////////////


  private def post[T](path: String, data: JsValue, isMd: Boolean)(parser: JsValue => T): Future[T] = {
    val request =
      Post(baseURL + path)
        .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`), data.toString))

    requestForJson(request, if (isMd) mdFlow else flow)(parser)
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
                                face: JumioImageRawData,
                                idType: EnumJumioDocTypes.JumioDocType,
                                idFront: JumioImageRawData,
                                idBack: Option[JumioImageRawData],
                                callbackUrl: String): Future[PerformNetverifyResponse] = {

    for {
      faceString <- encode(face.data)
      idFrontString <- encode(idFront.data)
      idBackString: Option[String] <- idBack.map(_.data).map(encode).map(_.map(Some(_))).getOrElse(Future.successful(None))
      params = PerformNetverifyRequest(
        merchantIdScanReference,
        faceString,
        face.contentType.mediaType.toString,
        idFrontString,
        idFront.contentType.mediaType.toString,
        idBackString,
        idBack.map(_.contentType.mediaType.toString),
        country,
        idType.toString,
        callbackUrl
      )
      request = Post(s"$baseURL/performNetverify")
        .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`), params.toJson.compactPrint))
      response <- (httpProxy ? request).mapTo[HttpClient.WrappedResponse].recover {
        case ex =>
          log.error(ex, s"Ask request to proxy actor with response to ${request.method.value} ${request.uri} failed")
          throw ex
      }.map(processStreamingResponse)
      result <- Unmarshal(response).to[String].map { r => r.parseJson.convertTo[PerformNetverifyResponse] }
      //      result <- post("/performNetverify", params.toJson, isMd = false) { response =>
      //        response.convertTo[PerformNetverifyResponse]
      //      }
    } yield result
  }

  private def encode(data: Source[ByteString, Any]): Future[String] = {
    data.runFold(ByteString.createBuilder) { case (builder, bs) =>
      builder.append(bs)
    }.map(builder => Base64.getEncoder.encodeToString(builder.result().toArray))
  }
}
