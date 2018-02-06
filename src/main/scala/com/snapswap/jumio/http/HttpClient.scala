package com.snapswap.jumio.http

import akka.actor.ActorRef
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}

import scala.util.{Failure, Success, Try}

object HttpClient {

  type StreamingConnection = Flow[(HttpRequest, ProxyMeta), (Try[HttpResponse], ProxyMeta), HostConnectionPool]

  case class ProxyMeta(proxyActor: ActorRef, pipeTo: ActorRef, request: HttpRequest)

  case class WrappedResponse(response: Either[Throwable, HttpResponse], meta: ProxyMeta)

  private def source(bufferSize: Int, overflowStrategy: OverflowStrategy) =
    Source.actorRef[(HttpRequest, ProxyMeta)](bufferSize, overflowStrategy)

  private def sink = Sink.foreach[(Try[HttpResponse], ProxyMeta)] {
    case (Success(response), meta) =>
      meta.proxyActor ! WrappedResponse(Right(response), meta)
    case (Failure(ex), data) =>
      data.proxyActor ! WrappedResponse(Left(ex), data)
  }

  def apply(connection: StreamingConnection,
            bufferSize: Int,
            overflowStrategy: OverflowStrategy)
           (implicit mat: Materializer): ActorRef =
    source(bufferSize, overflowStrategy)
      .via(connection)
      .to(sink)
      .run()
}