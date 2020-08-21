package com.snapswap.jumio.http

import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.Source
import com.snapswap.http.client.HttpClient
import com.snapswap.http.client.model.{EnrichedRequest, EnrichedResponse}
import com.snapswap.jumio.model.retrieval.{JumioImage, JumioImageRawData}

import scala.util.{Failure, Success}

trait JumioHttpImageClient {
  self: JumioHttpClient =>

  private implicit def toImmutableSeq[T](seq: Seq[T]): collection.immutable.Seq[T] =
    collection.immutable.Seq(seq).flatten

  final def getImages(img: Seq[JumioImage], client: HttpClient)
                     (implicit params: JumioRetrievalConnectionParams): Source[(JumioImageRawData, JumioImage), Any] = {
    client.send(Source(
      img.map { i =>
        EnrichedRequest(Get(Uri(i.href)).withHeaders(authHeaders), i)
      }
    )).map {
      case EnrichedResponse(Success(response), EnrichedRequest(_, imageInfo, _)) =>
        JumioImageRawData(response.entity.dataBytes, response.entity.contentType) -> imageInfo
      case EnrichedResponse(Failure(ex), EnrichedRequest(_, imageInfo, id)) =>
        log.error(ex, s"retrieval for $imageInfo failed, request id: $id")
        throw ex
    }
  }
}
