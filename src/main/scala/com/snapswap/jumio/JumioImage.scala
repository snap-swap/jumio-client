package com.snapswap.jumio

import akka.http.scaladsl.model.ContentType
import akka.stream.scaladsl.Source
import akka.util.ByteString

/**
  * maskhint parameter is used for credit cards only, see jumio retrieval api doc for more details
  **/
case class JumioImage(classifier: String,
                      href: String,
                      maskhint: Option[EnumJumioImageMaskHint.ImageMaskHint])

case class JumioImagesInfo(timestamp: String,
                           scanReference: String,
                           images: Seq[JumioImage])

object EnumJumioImageMaskHint extends Enumeration {
  type ImageMaskHint = Value

  val masked, unmasked = Value
}

case class RawImage(data: Source[ByteString, Any],
                    contentType: ContentType)