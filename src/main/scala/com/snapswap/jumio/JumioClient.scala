package com.snapswap.jumio

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try, Success, Failure}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, Sink}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, MediaTypes}
import akka.http.scaladsl.model.headers.{`User-Agent`, Accept, Authorization, BasicHttpCredentials}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json._

trait JumioClient {
  def scanStatus(scanReference: String): Future[JumioScanStatus]
  def scanDetails(scanReference: String): Future[JumioScan]
}

class AkkaHttpJumioClient(clientToken: String, clientSecret: String,
                          clientCompanyName: String, clientApplicationName: String, clientVersion: String,
                          apiHost: String = "lon.netverify.com",
                          maxRetries: Int = 10)
                         (implicit system: ActorSystem, ctx: ExecutionContext, materializer: Materializer)
  extends JumioClient {

  import unmarshaller._

  private val log = Logging(system, this.getClass)
  private val baseURL = s"/api/netverify/v2"
  private val userAgent = s"$clientCompanyName $clientApplicationName/$clientVersion"
  private val flow = Http().cachedHostConnectionPoolHttps[UUID](apiHost, 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio")

  private def send(request: HttpRequest): Future[JsValue] =
    Source.single(request -> UUID.randomUUID()).via(flow).runWith(Sink.head).recoverWith {
      case ex =>
        Future.failed(JumioConnectionError(s"${request.method.value} ${request.uri} failed: ${ex.getMessage}", Some(ex)))
    }.flatMap {
      case (Success(response), _) =>
        if (response.status == StatusCodes.NotFound) {
          log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status}")
          Future.failed(JumioEntityNotFoundError)
        } else {
          Unmarshal(response.entity).to[String].flatMap { responseEntity =>
            log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status} 'responseEntity'")
            Try(responseEntity.parseJson) match {
              case Success(json) => Future.successful(json)
              case Failure(ex) =>
                log.error(ex, s"Response to ${request.method.value} ${request.uri} isn't JSON (${ex.getMessage}): $responseEntity")
                Future.failed(JumioMalformedResponse(responseEntity, Some(ex)))
            }
          }
        }
      case (Failure(ex), _) =>
        log.error(ex, s"${request.method.value} ${request.uri} failed with ${ex.getMessage}")
        Future.failed(ex)
    }

  private def parameters[T](query: Map[String, String]): String =
    if (query.isEmpty) {
      ""
    } else {
      "?" + query.map(tup => s"${tup._1}=${tup._2}").mkString("&")
    }

  private def get[T](path: String, query: Map[String, String] = Map())(parser: JsValue => T): Future[T] = {
    val url = baseURL + path + parameters(query)
    val request = Get(url).withHeaders(`User-Agent`(userAgent), Accept(MediaTypes.`application/json`),
      Authorization(BasicHttpCredentials(clientToken, clientSecret)))
    send(request).flatMap(json => Try(parser(json)) match {
      case Success(result) => Future.successful(result)
      case Failure(ex) => Future.failed(JumioMalformedResponse(json.compactPrint, Some(ex)))
    })
  }

  def scanStatus(scanReference: String): Future[JumioScanStatus] = {
    get(s"/scans/$scanReference") { response =>
      response.convertTo[JumioScanStatus]
    }
  }

  def scanDetails(scanReference: String): Future[JumioScan] = {
    get(s"/scans/$scanReference/data") { response =>
      response.convertTo[JumioScan]
    }
  }
}
