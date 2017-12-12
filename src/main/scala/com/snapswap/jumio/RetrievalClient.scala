package com.snapswap.jumio

import akka.stream.scaladsl.Source
import com.snapswap.jumio.model.retrieval._

import scala.concurrent.Future


trait RetrievalClient {
  def scanStatus(scanReference: String): Future[JumioScanStatus]

  def scanDetails(scanReference: String): Future[JumioScan]

  def scanMdDetails(scanReference: String): Future[JumioScan]

  def scanImages(scanReference: String): Future[JumioImagesInfo]

  def mdScanImages(scanReference: String): Future[JumioImagesInfo]

  def obtainImage(images: Seq[JumioImage]): Source[(JumioImageRawData, JumioImage), Any]

  def obtainMdImage(images: Seq[JumioImage]): Source[(JumioImageRawData, JumioImage), Any]
}

