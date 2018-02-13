package com.snapswap.jumio.http

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{get, path}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import akka.util.Timeout
import com.snapswap.jumio.http.HttpClient.{ProxyMeta, WrappedResponse}
import com.typesafe.config.ConfigFactory
import org.scalatest.{AsyncWordSpecLike, BeforeAndAfterAll, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class HttpClientITSpec
  extends AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Server
    with Client {

  implicit val timeout: Timeout = Timeout(10.seconds)

  "HttpClient" should {
    "be able to perform a vast amount of requests" in {
      val requests = Seq.fill[HttpRequest](Config.numberOfRequests)(Get("/ping"))

      val result = Future.traverse(requests) { r =>
        (proxy ? r).mapTo[WrappedResponse].flatMap(processResponse)
      }

      result.map { responses =>
        val successful = responses.collect { case Right(r) if r == "pong" => r }
        successful.length shouldBe Config.numberOfRequests

        val exceptions = responses.collect { case Left(ex) => ex }
        exceptions.length shouldBe 0
      }
    }
    "in opposite to the Source.singe approach" in {
      val requests = Seq.fill[HttpRequest](Config.numberOfRequests)(Get("/ping"))

      val result = Future.traverse(requests) { r =>
        Source.single(r -> ProxyMeta(ActorRef.noSender, ActorRef.noSender, r))
          .via(connection)
          .runWith(Sink.head)
          .flatMap(processResponse)
      }

      result.map { responses =>
        val successful = responses.collect { case Right(r) if r == "pong" => r }
        successful.length should be < Config.numberOfRequests

        val exceptions = responses.collect { case Left(ex: akka.stream.BufferOverflowException) => ex }
        exceptions.length should be > 0
      }
    }
  }

}


object Config {
  val host = "0.0.0.0"
  val port = 8000
  val responseDelay = 10.millis
  val numberOfRequests = 1000

  val serverConfig =
    """
      |akka{
      | log-level = "DEBUG"
      | http.server {
      |   max-connections = 2048
      | }
      |}
    """.stripMargin

  val clientConfig =
    """
      |akka{
      | log-level = "DEBUG"
      | http.host-connection-pool {
      |   max-open-requests = 8
      | }
      |}
    """.stripMargin
}


trait Client {
  private implicit val clientSystem: ActorSystem = ActorSystem("test-client", ConfigFactory.parseString(Config.clientConfig))
  implicit val clientMat: Materializer = ActorMaterializer()
  private implicit val _: ExecutionContext = clientSystem.dispatcher

  val connection: HttpClient.StreamingConnection =
    Http().cachedHostConnectionPool[HttpClient.ProxyMeta](Config.host, Config.port)

  private val client: ActorRef = HttpClient.apply(connection, Int.MaxValue, OverflowStrategy.dropNew)

  val proxy: ActorRef = clientSystem.actorOf(HttpClientProxy.props(client))

  def processResponse(r: WrappedResponse): Future[Either[Throwable, String]] =
    processResponse(r.response)

  def processResponse(r: (Try[HttpResponse], ProxyMeta)): Future[Either[Throwable, String]] = r match {
    case (Success(response), _) =>
      processResponse(Right(response))
    case (Failure(ex), _) =>
      processResponse(Left(ex))
  }

  private def processResponse(r: Either[Throwable, HttpResponse]): Future[Either[Throwable, String]] = r match {
    case Left(ex) =>
      Future.successful(Left(ex))
    case Right(response) if response.status != StatusCodes.OK =>
      Future.successful(Left(new RuntimeException(s"response was unsuccessful ${response.status}")))
    case Right(response) =>
      Unmarshal(response).to[String].map(Right(_))
  }
}


trait Server extends FutureTimeoutSupport {
  private implicit val serverSystem: ActorSystem = ActorSystem("test-server", ConfigFactory.parseString(Config.serverConfig))
  private implicit val serverMat: Materializer = ActorMaterializer()
  private implicit val _: ExecutionContext = serverSystem.dispatcher

  private val serverRoute: Route = get {
    path("ping") {
      Directives.complete {
        after[HttpResponse](Config.responseDelay, serverSystem.scheduler) {
          Future.successful(HttpResponse(StatusCodes.OK, entity = "pong"))
        }
      }
    }
  }

  Http().bindAndHandle(serverRoute, Config.host, Config.port)

}