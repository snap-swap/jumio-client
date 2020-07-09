package com.snapswap.jumio

import akka.stream.scaladsl.Source
import com.snapswap.jumio.http.JumioRetrievalConnectionParams
import com.snapswap.jumio.model.retrieval._

import scala.concurrent.Future


trait RetrievalClient {
  def scanStatus(scanReference: String)
                (implicit params: JumioRetrievalConnectionParams): Future[JumioScanStatus]

  def scanDetails(scanReference: String)
                 (implicit params: JumioRetrievalConnectionParams): Future[JumioScan]

  def scanImages(scanReference: String)
                (implicit params: JumioRetrievalConnectionParams): Future[JumioImagesInfo]

  def obtainImage(images: Seq[JumioImage])
                 (implicit params: JumioRetrievalConnectionParams): Source[(JumioImageRawData, JumioImage), Any]

}

