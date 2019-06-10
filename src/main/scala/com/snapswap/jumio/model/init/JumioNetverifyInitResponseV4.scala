package com.snapswap.jumio.model.init

import java.time.ZonedDateTime

case class JumioNetverifyInitResponseV4(transactionReference: String, //the same as jumioIdScanReference in v3
                                        redirectUrl: String,
                                        timestamp: ZonedDateTime)