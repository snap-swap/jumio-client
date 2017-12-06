package com.snapswap.jumio.model.init

import java.time.ZonedDateTime


case class JumioNetverifyInitResponse(authorizationToken: String,
                                      jumioIdScanReference: String,
                                      timestamp: ZonedDateTime)
