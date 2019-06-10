package com.snapswap.jumio.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, BasicHttpCredentials, `User-Agent`}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.util.Timeout
import com.snapswap.http.client.HttpClient
import com.snapswap.jumio.model.errors.{JumioConnectionError, JumioEntityNotFoundError, JumioMalformedResponse}
import spray.json.{JsValue, _}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


private[http] trait JumioHttpClient {

  def clientToken: String

  def clientSecret: String

  def clientCompanyName: String

  def clientApplicationName: String

  def clientVersion: String

  def log: LoggingAdapter

  implicit def system: ActorSystem

  implicit def ctx: ExecutionContext

  implicit def materializer: Materializer

  implicit val responseAwaitingTimeout: Timeout = Timeout(1.minute)

  implicit def headers(h: Seq[HttpHeader]): collection.immutable.Seq[HttpHeader] =
    collection.immutable.Seq(h).flatten

  protected def userAgent: String =
    s"$clientCompanyName $clientApplicationName/$clientVersion"

  protected def authHeaders = Seq(
    `User-Agent`(userAgent),
    Authorization(BasicHttpCredentials(clientToken, clientSecret))
  )

  protected def send[T](request: HttpRequest, client: HttpClient[NotUsed])
                       (transform: ResponseEntity => Future[T]): Future[T] =
    client.send(request).recoverWith {
      case ex =>
        Future.failed(JumioConnectionError(s"${request.method.value} ${request.uri} failed: ${ex.getMessage}", ex))
    }.flatMap {
      case Success(response) =>
        processResponse(request, response, transform)
      case Failure(ex) =>
        log.error(ex, s"${request.method.value} ${request.uri} failed with ${ex.getMessage}")
        Future.failed(ex)
    }


  private def asJson(r: ResponseEntity): Future[JsValue] =
    Unmarshal(r).to[String].map { s =>
      Try(s.parseJson) match {
        case Success(json) =>
          json
        case Failure(ex) =>
          log.error(ex, s"Given value isn't JSON (${ex.getMessage}): $s")
          throw JumioMalformedResponse(s, ex)
      }
    }

  protected def requestForJson[T](request: HttpRequest, client: HttpClient[NotUsed])(parse: JsValue => T): Future[T] =
    send(request.withHeaders(authHeaders :+ Accept(MediaTypes.`application/json`)), client)(asJson)
      .map(v => parseJsValue(v, parse))

  private def processResponse[T](request: HttpRequest, response: HttpResponse, transform: ResponseEntity => Future[T]): Future[T] = {
    if (response.status == StatusCodes.NotFound) {
      response.discardEntityBytes()
      log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status}")
      Future.failed(JumioEntityNotFoundError)
    } else {
      log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status}")
      transform(response.entity)
    }
  }

  private def parseJsValue[T](jsValue: JsValue, parse: JsValue => T): T = {
    Try(parse(jsValue)) match {
      case Success(result) =>
        result
      case Failure(ex) =>
        throw JumioMalformedResponse(jsValue.compactPrint, ex)
    }
  }

}
