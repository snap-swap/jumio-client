package com.snapswap.jumio.model.netverify

import java.time.ZonedDateTime

import com.snapswap.jumio.model.{EnumJumioDocumentStatus, EnumJumioSources, JumioFailureResult}
import com.snapswap.jumio.utils.OptionObjUtils


case class JumioScanFailure(scanReference: String,
                            status: EnumJumioDocumentStatus.JumioDocumentStatus,
                            source: EnumJumioSources.JumioSource,
                            checks: JumioChecks,
                            timestamp: Option[ZonedDateTime],
                            callbackTimestamp: Option[ZonedDateTime],
                            rejectReason: JumioRejectReason,
                            merchantScanReference: String,
                            customerId: Option[String],
                            clientIp: Option[String],
                            links: JumioScanLinks) extends JumioScanResult with JumioFailureResult {
  override def toString: String = {
    import OptionObjUtils._

    s"$status $scanReference (merchantScanID=$merchantScanReference, customerID=${customerId.toStringOrNA}) " +
      s"from $source at IP ${clientIp.toStringOrNA}, scanned at ${timestamp.toStringOrNA}, completed at ${callbackTimestamp.toStringOrNA}, " +
      s"$checks, URLs [$links]: rejected as $rejectReason"
  }

  override def error: String = status.toString

  override def details: String = rejectReason.toString
}

