package com.snapswap.jumio.model.retrieval

/**
  * maskhint parameter is used for credit cards only, see jumio retrieval api doc for more details
  **/
case class JumioImage(classifier: String,
                      href: String,
                      maskhint: Option[EnumJumioImageMaskHint.ImageMaskHint])




