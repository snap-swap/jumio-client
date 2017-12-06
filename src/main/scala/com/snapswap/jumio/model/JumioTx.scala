package com.snapswap.jumio.model

import java.time.ZonedDateTime


case class JumioTx(status: EnumJumioTxStatuses.JumioTxStatus,
                   optSource: Option[EnumJumioSources.JumioSource],
                   date: Option[ZonedDateTime],
                   clientIp: Option[String],
                   customerId: Option[String],
                   additionalInformation: Option[String],
                   merchantScanReference: Option[String],
                   merchantReportingCriteria: Option[String]) {

  val source: EnumJumioSources.JumioSource = optSource.getOrElse(EnumJumioSources.unknown)

  override def toString: String =
    (
      Some(s"$status, received at $date from $source") ::
        clientIp.map(v => s" $v IP") ::
        customerId.map(v => s" of $v customer") ::
        merchantScanReference.map(v => s", $v merchant scan ref") ::
        merchantReportingCriteria.map(v => s", $v merchant reporting criteria") ::
        additionalInformation.map(v => s", '$v'") :: Nil
      ).flatten.mkString
}