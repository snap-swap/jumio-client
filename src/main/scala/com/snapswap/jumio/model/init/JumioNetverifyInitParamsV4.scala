package com.snapswap.jumio.model.init

case class JumioNetverifyInitParamsV4(customerInternalReference: String, //the same as merchantIdScanReference in v3
                                      successUrl: String,
                                      errorUrl: String,
                                      callbackUrl: String,
                                      userReference: String, //the same as customerId in v3
                                      locale: Option[String])
