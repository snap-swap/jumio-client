package com.snapswap.jumio.model.netverify

import java.time.ZonedDateTime

import com.snapswap.jumio.model._
import com.snapswap.jumio.utils.OptionObjUtils

case class JumioScanSuccess(scanReference: String,
                            status: EnumJumioDocumentStatus.JumioDocumentStatus,
                            source: EnumJumioSources.JumioSource,
                            checks: JumioChecks,
                            timestamp: Option[ZonedDateTime],
                            callbackTimestamp: Option[ZonedDateTime],
                            document: JumioDocument,
                            merchantScanReference: String,
                            customerId: Option[String],
                            clientIp: Option[String],
                            additionalInformation: String,
                            links: JumioScanLinks,
                            identityVerification: Option[IdentityVerification]) extends JumioScanResult {
  override def toString: String = {
    import OptionObjUtils._

    s"$status $scanReference (merchantScanID=$merchantScanReference, customerID=${customerId.toStringOrNA}) " +
      s"from $source at IP ${clientIp.toStringOrNA}, scanned at ${timestamp.toStringOrNA}, completed at ${callbackTimestamp.toStringOrNA}, " +
      s"$checks, " +
      s"identity verification ${identityVerification.toStringOrNA}" +
      s", URLs [$links]: $document"
  }
}