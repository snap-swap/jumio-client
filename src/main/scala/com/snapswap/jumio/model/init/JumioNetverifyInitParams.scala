package com.snapswap.jumio.model.init


case class JumioNetverifyInitParams(merchantScanReference: String,
                                    successUrl: String,
                                    errorUrl: String,
                                    callbackUrl: String,
                                    customerId: String)
