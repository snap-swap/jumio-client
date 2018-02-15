package com.snapswap.jumio.http

import akka.http.scaladsl.client.RequestBuilding._
import akka.stream.scaladsl.Source
import com.snapswap.http.client.HttpClient
import com.snapswap.jumio.model.retrieval.{JumioImage, JumioImageRawData}

import scala.util.{Failure, Success}

trait JumioHttpImageClient {
  self: JumioHttpClient =>

  private implicit def toImmutableSeq[T](seq: Seq[T]): collection.immutable.Seq[T] =
    collection.immutable.Seq(seq).flatten

  final def getImages(img: Seq[JumioImage],
                      client: HttpClient): Source[(JumioImageRawData, JumioImage), Any] = {
    client.send(
      Source(img.map { i =>
        Get(i.href).withHeaders(authHeaders) -> i
      })
    ).map {
      case (Success(response), imageInfo) =>
        JumioImageRawData(response.entity.dataBytes, response.entity.contentType) -> imageInfo
      case (Failure(ex), imageInfo) =>
        log.error(ex, s"retrieval for $imageInfo failed with ${ex.getMessage}")
        throw ex
    }
  }
}
