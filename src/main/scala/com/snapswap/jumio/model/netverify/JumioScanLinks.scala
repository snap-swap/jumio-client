package com.snapswap.jumio.model.netverify

import akka.http.scaladsl.model.Uri


case class JumioScanLinks(frontImage: Option[Uri], backImage: Option[Uri], faceImage: Option[Uri]) {
  override def toString: String = {
    def urlOrNA(uri: Option[Uri]) = uri.map(_.toString()).getOrElse("N/A")

    ("idScanImage=" + urlOrNA(frontImage) ::
      "idScanImageBackside=" + urlOrNA(backImage) ::
      "idScanImageFace=" + urlOrNA(faceImage) ::
      Nil).mkString(", ")
  }
}

object JumioScanLinks {
  def of(parameters: Map[String, String]): JumioScanLinks = {
    def getUri(name: String): Option[Uri] = parameters.get(name).map(Uri.apply)

    val idScanImage = getUri("idScanImage")
    val idScanImageBackside = getUri("idScanImageBackside")
    val idScanImageFace = getUri("idScanImageFace")

    new JumioScanLinks(idScanImage, idScanImageBackside, idScanImageFace)
  }

  def of(str: String): JumioScanLinks = of(str.split(',').map(_.trim).flatMap { kv =>
    val arr = kv.split('=')
    val key = if (arr.length > 0) Some(arr(0)) else None
    val value = if (arr.length > 1) Some(arr(1)) else None
    key.flatMap(k => value.map(v => k -> v))
  }.toMap)
}
