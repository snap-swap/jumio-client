package com.snapswap.jumio

import com.snapswap.jumio.model.retrieval.{JumioImageRawData, JumioImagesInfo, JumioScan, JumioScanStatus}

import scala.concurrent.Future


trait RetrievalClient {
  def scanStatus(scanReference: String): Future[JumioScanStatus]

  def scanDetails(scanReference: String): Future[JumioScan]

  def scanMdDetails(scanReference: String): Future[JumioScan]

  def scanImages(scanReference: String): Future[JumioImagesInfo]

  def mdScanImages(scanReference: String): Future[JumioImagesInfo]

  def obtainImage(href: String): Future[JumioImageRawData]

  def obtainMdImage(href: String): Future[JumioImageRawData]
}

