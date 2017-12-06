package com.snapswap.jumio.model.retrieval

import java.time.ZonedDateTime

import com.snapswap.jumio.model.EnumJumioTxStatuses


case class JumioScanStatus(timestamp: ZonedDateTime,
                           scanReference: String,
                           status: EnumJumioTxStatuses.JumioTxStatus) {
  override def toString: String = s"$timestamp: '$scanReference' is $status"
}
