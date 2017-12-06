package com.snapswap.jumio.model.retrieval

import java.time.ZonedDateTime

import com.snapswap.jumio.model.{JumioDocument, JumioTx}


case class JumioScan(timestamp: ZonedDateTime,
                     scanReference: String,
                     transaction: JumioTx,
                     document: JumioDocument,
                     verification: Option[JumioVerification]) {
  override def toString: String = s"timestamp: '$scanReference' is $transaction"
}







