package com.snapswap.jumio.model.netverify

import spray.json._
import com.snapswap.jumio.json.protocol.JumioUnmarshaller._

case class PerformNetverifyRequest(merchantIdScanReference: String,
                                   faceImage: Option[String],
                                   faceImageMimeType: Option[String],
                                   frontsideImage: String,
                                   frontsideImageMimeType: String,
                                   backsideImage: Option[String],
                                   backsideImageMimeType: Option[String],
                                   country: String,
                                   idType: String,
                                   callbackUrl: String,
                                   enabledFields: String,
                                   customerId: Option[String],
                                   clientIp: Option[String]) {
  override def toString: String = {
    def shorten(str: String): String = s"${str.substring(0, 3)}..."
    copy(
      faceImage = faceImage.map(shorten),
      frontsideImage = shorten(frontsideImage),
      backsideImage = backsideImage.map(shorten)
    ).toJson.compactPrint
  }
}
