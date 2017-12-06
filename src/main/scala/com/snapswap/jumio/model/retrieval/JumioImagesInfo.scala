package com.snapswap.jumio.model.retrieval


case class JumioImagesInfo(timestamp: String,
                           scanReference: String,
                           images: Seq[JumioImage])
