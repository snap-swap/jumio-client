package com.snapswap.jumio.http

import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Source}
import com.snapswap.jumio.model.retrieval.{JumioImage, JumioImageRawData}

import scala.util.{Failure, Success, Try}

trait JumioHttpImageClient {
  self: JumioHttpClient =>

  final type ImageConnection = Flow[(HttpRequest, JumioImage), (Try[HttpResponse], JumioImage), HostConnectionPool]

  private implicit def toImmutableSeq[T](seq: Seq[T]): collection.immutable.Seq[T] =
    collection.immutable.Seq(seq).flatten

  final def getImages(img: Seq[JumioImage],
                      connection: ImageConnection): Source[(JumioImageRawData, JumioImage), Any] = {
    Source(img.map { i =>
      Get(i.href).withHeaders(authHeaders) -> i
    }).via(connection)
      .map {
        case (Success(response), imageInfo) =>
          JumioImageRawData(response.entity.withoutSizeLimit().dataBytes, response.entity.contentType) -> imageInfo
        case (Failure(ex), imageInfo) =>
          log.error(ex, s"retrieval for $imageInfo failed with ${ex.getMessage}")
          throw ex
      }
  }
}
