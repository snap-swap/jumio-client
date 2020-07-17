package com.snapswap.jumio.http

import akka.NotUsed
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.Source
import com.snapswap.http.client.HttpClient
import com.snapswap.jumio.model.retrieval.{JumioImage, JumioImageRawData}

import scala.util.{Failure, Success}

trait JumioHttpImageClient {
  self: JumioHttpClient =>

  private implicit def toImmutableSeq[T](seq: Seq[T]): collection.immutable.Seq[T] =
    collection.immutable.Seq(seq).flatten

  final def getImages(img: Seq[JumioImage], client: HttpClient[NotUsed])
                     (implicit params: JumioRetrievalConnectionParams): Source[(JumioImageRawData, JumioImage), Any] = {
    client.send(
      Source(img.map { i =>
        Get(Uri(params.apiHost + i.href)).withHeaders(authHeaders) -> i
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
