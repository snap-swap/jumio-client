package com.snapswap.jumio

import java.time.ZonedDateTime


case class JumioNetverifyInitParams(merchantScanReference: String,
                                    successUrl: String,
                                    errorUrl: String,
                                    callbackUrl: String,
                                    customerId: String)

case class JumioNetverifyInitResponse(authorizationToken: String,
                                      jumioIdScanReference: String,
                                      timestamp: ZonedDateTime)