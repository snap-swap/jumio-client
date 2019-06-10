package com.snapswap.jumio.model.init


case class JumioNetverifyInitParamsV3(merchantScanReference: String,
                                      successUrl: String,
                                      errorUrl: String,
                                      callbackUrl: String,
                                      customerId: String)
