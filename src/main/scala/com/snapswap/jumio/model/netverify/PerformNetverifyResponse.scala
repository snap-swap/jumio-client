package com.snapswap.jumio.model.netverify

import java.time.ZonedDateTime

case class PerformNetverifyResponse(jumioIdScanReference: String,
                                    timestamp: ZonedDateTime)