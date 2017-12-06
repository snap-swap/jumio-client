package com.snapswap.jumio.model.retrieval

import akka.http.scaladsl.model.ContentType
import akka.stream.scaladsl.Source
import akka.util.ByteString


case class JumioImageRawData(data: Source[ByteString, Any],
                             contentType: ContentType)
