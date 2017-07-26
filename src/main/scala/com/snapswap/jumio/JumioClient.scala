package com.snapswap.jumio


import java.util.UUID

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, BasicHttpCredentials, `User-Agent`}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json.{JsValue, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait JumioClient {
  protected type Connection = Flow[(HttpRequest, UUID), (Try[HttpResponse], UUID), HostConnectionPool]

  def clientToken: String

  def clientSecret: String

  def clientCompanyName: String

  def clientApplicationName: String

  def clientVersion: String

  def log: LoggingAdapter

  def useConnectionPool: Boolean

  implicit def system: ActorSystem

  implicit def ctx: ExecutionContext

  implicit def materializer: Materializer


  implicit def headers(h: Seq[HttpHeader]): collection.immutable.Seq[HttpHeader] =
    collection.immutable.Seq(h).flatten

  protected def userAgent: String =
    s"$clientCompanyName $clientApplicationName/$clientVersion"

  protected def authHeaders = Seq(
    `User-Agent`(userAgent),
    Authorization(BasicHttpCredentials(clientToken, clientSecret))
  )

  protected def send[T](request: HttpRequest, connection: Connection)
                       (transform: ResponseEntity => Future[T]): Future[T] =
    Source.single(request -> UUID.randomUUID()).via(connection).runWith(Sink.head)
      .recoverWith {
        case ex =>
          Future.failed(JumioConnectionError(s"${request.method.value} ${request.uri} failed: ${ex.getMessage}", Some(ex)))
      }.flatMap {
      case (Success(response), _) if response.status == StatusCodes.NotFound =>
        log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status}")
        Future.failed(JumioEntityNotFoundError)
      case (Failure(ex), _) =>
        log.error(ex, s"${request.method.value} ${request.uri} failed with ${ex.getMessage}")
        Future.failed(ex)
      case (Success(response), _) =>
        log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status}")
        transform(response.entity)
    }


  private def asJson(r: ResponseEntity): Future[JsValue] =
    Unmarshal(r).to[String].map { s =>
      Try(s.parseJson) match {
        case Success(json) =>
          json
        case Failure(ex) =>
          log.error(ex, s"Given value isn't JSON (${ex.getMessage}): $s")
          throw JumioMalformedResponse(s, Some(ex))
      }
    }

  protected def requestForJson[T](request: HttpRequest, connection: Connection)(parse: JsValue => T): Future[T] =
    send(request.withHeaders(authHeaders :+ Accept(MediaTypes.`application/json`)), connection)(asJson)
      .map(parseJsValue(_, parse))

  protected def send[T](request: HttpRequest, http: HttpExt)
                       (transform: ResponseEntity => Future[T]): Future[T] = {
    http
      .singleRequest(request, log = log)
      .recover {
        case NonFatal(ex) =>
          throw JumioConnectionError(s"${request.method.value} ${request.uri} failed: ${ex.getMessage}", Some(ex))
      }.flatMap {
      case response if response.status == StatusCodes.NotFound =>
        log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status}")
        Future.failed(JumioEntityNotFoundError)
      case response =>
        log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status}")
        transform(response.entity)
    }
  }

  protected def requestForJson[T](request: HttpRequest, http: HttpExt)(parse: JsValue => T): Future[T] = {
    send(request.withHeaders(authHeaders :+ Accept(MediaTypes.`application/json`)), http)(asJson)
      .map(parseJsValue(_, parse))
  }

  private def parseJsValue[T](jsValue: JsValue, parse: JsValue => T): T = {
    Try(parse(jsValue)) match {
      case Success(result) =>
        result
      case Failure(ex) =>
        throw JumioMalformedResponse(jsValue.compactPrint, Some(ex))
    }
  }

}
