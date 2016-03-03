package com.snapswap.jumio

import org.joda.time.DateTime

object EnumJumioTxStatuses extends Enumeration {
  type JumioTxStatus = Value
  val pending = Value("PENDING")
  val done = Value("DONE")
  val failed = Value("FAILED")
}

object EnumJumioSources extends Enumeration {
  type JumioSource = Value
  // Netverify Web embedded
  val web = Value("WEB")
  val web_cam = Value("WEB-CAM")
  val web_upload = Value("WEB-UPLOAD")

  // Netverify Web redirect
  val redirect = Value("REDIRECT")
  val redirect_cam = Value("REDIRECT-CAM")
  val redirect_upload = Value("REDIRECT-UPLOAD")

  // performNetverify
  val api = Value("API")

  //Netverify Mobile
  val sdk = Value("SDK")
}

object EnumJumioMRZCheck extends Enumeration {
  type JumioMRZCheck = Value
  val ok = Value("OK")
  val not_ok = Value("NOT_OK")
  val not_available = Value("NOT_AVAILABLE")
}

case class JumioScanStatus(timestamp: DateTime, scanReference: String, status: EnumJumioTxStatuses.JumioTxStatus) {
  override def toString = s"$timestamp: '$scanReference' is $status"
}

case class JumioScan(timestamp: DateTime, scanReference: String,
                     transaction: JumioTx,
                     document: JumioDocument,
                     verification: Option[JumioVerification]) {
  override def toString = s"timestamp: '$scanReference' is $transaction"
}

case class JumioTx(status: EnumJumioTxStatuses.JumioTxStatus, source: EnumJumioSources.JumioSource, date: DateTime,
                   clientIp: Option[String], customerId: Option[String], additionalInformation: Option[String],
                   merchantScanReference: Option[String], merchantReportingCriteria: Option[String]) {
  override def toString =
    (Some(s"$status, received at $date from $source") ::
      clientIp.map(v => s" $v IP") ::
      customerId.map(v => s" of $v customer") ::
      merchantScanReference.map(v => s", $v merchant scan ref") ::
      merchantReportingCriteria.map(v => s", $v merchant reporting criteria") ::
      additionalInformation.map(v => s", '$v'") :: Nil).flatten.mkString
}

case class JumioRejectReason(code: String, description: String) {
  override def toString = s"[$code] $description"
}

case class JumioVerification(mrzCheck: Option[EnumJumioMRZCheck.JumioMRZCheck],
                             faceMatchPercentage: Option[Int],
                             rejectReason: Option[JumioRejectReason]) {
  def rejected: Boolean = rejectReason.isDefined
}
