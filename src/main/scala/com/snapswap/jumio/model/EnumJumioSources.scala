package com.snapswap.jumio.model


object EnumJumioSources extends Enumeration {
  type JumioSource = Value
  // Netverify Web embedded
  val web = Value("WEB")
  val web_cam = Value("WEB_CAM")
  val web_upload = Value("WEB_UPLOAD")

  // Netverify Web redirect
  val redirect = Value("REDIRECT")
  val redirect_cam = Value("REDIRECT_CAM")
  val redirect_upload = Value("REDIRECT_UPLOAD")

  // performNetverify
  val api = Value("API")

  //Netverify Mobile
  val sdk = Value("SDK")

  val doc_api = Value("DOC_API")

  val unknown = Value("UNKNOWN")

  //Netverify multi document
  val doc_upload = Value("DOC_UPLOAD")
}
