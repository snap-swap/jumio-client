package com.snapswap.jumio

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, BasicHttpCredentials, `User-Agent`}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait JumioClient {
  type RawImage = Source[ByteString, Any]

  def scanStatus(scanReference: String): Future[JumioScanStatus]

  def scanDetails(scanReference: String): Future[JumioScan]

  def initNetverify(merchantScanReference: String,
                    redirectUrl: String,
                    callbackUrl: String,
                    customerId: String): Future[JumioNetverifyInitResponse]

  def initMdNetverify(merchantScanReference: String,
                      redirectUrl: String,
                      callbackUrl: String,
                      customerId: String,
                      country: String,
                      docType: EnumJumioDocTypes.JumioDocType): Future[JumioMdNetverifyInitResponse]

  def scanImages(scanReference: String): Future[JumioImagesInfo]

  def mdScanImages(scanReference: String): Future[JumioImagesInfo]

  def obtainImage(href: String): Future[RawImage]

  def obtainMdImage(href: String): Future[RawImage]
}

/**
  * Attention! Credentials for netverify and retrieval netverify APIs are different
  **/
class AkkaHttpJumioClient(clientToken: String, clientSecret: String,
                          clientCompanyName: String, clientApplicationName: String, clientVersion: String,
                          apiHost: String = "lon.netverify.com",
                          maxRetries: Int = 10)
                         (implicit system: ActorSystem, ctx: ExecutionContext, materializer: Materializer)
  extends JumioClient {

  import unmarshaller._

  private type Connection = Flow[(HttpRequest, UUID), (Try[HttpResponse], UUID), HostConnectionPool]
  private val log = Logging(system, this.getClass)
  private val baseURL = s"/api/netverify/v2"
  private val userAgent = s"$clientCompanyName $clientApplicationName/$clientVersion"

  private val flow: Connection = Http().cachedHostConnectionPoolHttps[UUID](apiHost, 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio")

  private val mdFlow: Connection = Http().cachedHostConnectionPoolHttps[UUID](s"upload.$apiHost", 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio multi document")

  private val retrievalMdFlow: Connection = Http().cachedHostConnectionPoolHttps[UUID](s"retrieval.$apiHost", 443,
    settings = ConnectionPoolSettings(system).withMaxRetries(maxRetries)).log("jumio multi document retrieval")


  implicit def headers(h: Seq[HttpHeader]): collection.immutable.Seq[HttpHeader] =
    collection.immutable.Seq(h).flatten

  private val authHeaders = Seq(
    `User-Agent`(userAgent),
    Authorization(BasicHttpCredentials(clientToken, clientSecret))
  )


  private def send[T](request: HttpRequest, connection: Connection)(transform: ResponseEntity => Future[T]): Future[T] =
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
        log.debug(s"Response to ${request.method.value} ${request.uri} is ${response.status} 'responseEntity'")
        transform(response.entity)
    }

  private def responseToJson(r: ResponseEntity): Future[JsValue] =
    Unmarshal(r).to[String].map { s =>
      Try(s.parseJson) match {
        case Success(json) =>
          json
        case Failure(ex) =>
          log.error(ex, s"Given value isn't JSON (${ex.getMessage}): $s")
          throw JumioMalformedResponse(s, Some(ex))
      }
    }

  private def requestForJson[T](request: HttpRequest, connection: Connection)(parse: JsValue => T): Future[T] =
    send(request.withHeaders(authHeaders :+ Accept(MediaTypes.`application/json`)), connection)(responseToJson).map { r =>
      Try(parse(r)) match {
        case Success(result) =>
          result
        case Failure(ex) =>
          throw JumioMalformedResponse(r.compactPrint, Some(ex))
      }
    }

  private def requestForImage(request: HttpRequest, connection: Connection): Future[Source[ByteString, Any]] =
    send(request.withHeaders(authHeaders), connection) { r =>
      Future.successful(r.withContentType(MediaTypes.`image/jpeg`).dataBytes)
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

  private def post[T](path: String, connection: Connection, data: JsValue)(parser: JsValue => T): Future[T] = {
    val request =
      Post(baseURL + path)
        .withEntity(HttpEntity(ContentType(MediaTypes.`application/json`), data.toString))

    requestForJson(request, connection)(parser)
  }


  override def initNetverify(merchantScanReference: String,
                             redirectUrl: String,
                             callbackUrl: String,
                             customerId: String): Future[JumioNetverifyInitResponse] = {
    val params = JumioNetverifyInitParams(merchantScanReference, redirectUrl, redirectUrl, callbackUrl, customerId)
    post("/initiateNetverify", flow, params.toJson) { response =>
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
    post("/acquisitions", mdFlow, params.toJson) { response =>
      response.convertTo[JumioMdNetverifyInitResponse]
    }
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

  override def scanImages(scanReference: String): Future[JumioImagesInfo] =
    get(s"/scans/$scanReference/images", flow) { response =>
      response.convertTo[JumioImagesInfo]
    }

  override def mdScanImages(scanReference: String): Future[JumioImagesInfo] =
    get(s"/documents/$scanReference/pages", retrievalMdFlow) { response =>
      response.convertTo[JumioImagesInfo]
    }

  override def obtainImage(href: String): Future[RawImage] =
    requestForImage(Get(href), flow)

  override def obtainMdImage(href: String): Future[RawImage] =
    requestForImage(Get(href), retrievalMdFlow)

}


//object RetrieveImageUsageExample extends App {
//
//  import java.nio.file.Paths
//
//  import akka.stream.scaladsl.FileIO
//  import akka.stream.{ActorMaterializer, IOResult}
//
//
//  implicit val system = ActorSystem("test")
//  implicit val materializer = ActorMaterializer()
//  implicit val context = ExecutionContext.global
//  val log = Logging(system, this.getClass)
//
//  //use retrieval api credentials here
//  val client = new AkkaHttpJumioClient(
//    clientToken = "",
//    clientSecret = "",
//    clientCompanyName = "snapswap",
//    clientApplicationName = "remote-kyc",
//    clientVersion = "v1",
//    apiHost = "netverify.com"
//  )
//
//
//  def fileWriter(bss: JumioClient#RawImage, path: String): Future[IOResult] =
//    bss.runWith(FileIO.toPath(Paths.get(path)))
//
//  def saveImagesForJumioScan(scanReference: String, dir: String)
//                            (getImageInfoMethod: String => Future[JumioImagesInfo],
//                             obtainImageMethod: String => Future[JumioClient#RawImage]): Future[Unit] = (for {
//    scan <- getImageInfoMethod(scanReference)
//    doneImages = scan.images.map { case JumioImage(classifier, href, _) =>
//      obtainImageMethod(href).flatMap { response =>
//        val fileName = s"$dir$scanReference.$classifier.jpg"
//        fileWriter(response, fileName).map { ioResult =>
//          (fileName, ioResult)
//        }
//      }
//    }
//    result <- Future.sequence(doneImages)
//  } yield result.foreach {
//    case (f, r) if r.wasSuccessful =>
//      log.info(s"scan image for scanReference $scanReference was saved successfully as $f")
//    case _ =>
//      log.error(s"!!! ATTENTION !!! there are some problems during saving scan for scanReference $scanReference")
//  }).recover {
//    case ex =>
//      Future.successful(log.error(s"${ex.getClass.getSimpleName}: ${ex.getMessage}"))
//  }
//
//
//  val scanReference = "b4aa577c-c46f-42fd-9c2e-e68bb44d49d4"
//  val mdScanReference = "061e452d-311a-47cb-b4b4-c4af3a03b6ed"
//
//  val getFiles = saveImagesForJumioScan(scanReference, "")(client.scanImages, client.obtainImage)
//  val getMdFiles = saveImagesForJumioScan(mdScanReference, "")(client.mdScanImages, client.obtainMdImage)
//
//  for {
//    _ <- getFiles
//    _ <- getMdFiles
//    _ <- system.terminate()
//  } yield ()
//}
