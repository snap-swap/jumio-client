package com.snapswap.jumio.model.init

import java.time.ZonedDateTime


case class JumioNetverifyInitResponseV3(authorizationToken: String,
                                        jumioIdScanReference: String,
                                        timestamp: ZonedDateTime)
