package com.snapswap.jumio

import org.joda.time.DateTime


case class JumioNetverifyInitParams(merchantScanReference: String,
                                    successUrl: String,
                                    errorUrl: String,
                                    customerId: String)

case class JumioNetverifyInitResponse(authorizationToken: String,
                                      jumioIdScanReference: String,
                                      timestamp: DateTime)