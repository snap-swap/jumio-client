package com.snapswap.jumio.model.init


case class JumioNetverifyInitParamsV3(merchantScanReference: String,
                                      successUrl: Option[String],
                                      errorUrl: Option[String],
                                      callbackUrl: String,
                                      customerId: String)
