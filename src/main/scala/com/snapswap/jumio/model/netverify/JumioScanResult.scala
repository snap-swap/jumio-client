package com.snapswap.jumio.model.netverify

import java.time.ZonedDateTime

import com.snapswap.jumio.json.protocol.JumioUnmarshaller
import com.snapswap.jumio.model._
import com.snapswap.jumio.utils.OptionStringUtils
import spray.json._

trait JumioScanResult extends JumioResult {
  override def scanReference: String

  def status: EnumJumioDocumentStatus.JumioDocumentStatus

  override def source: EnumJumioSources.JumioSource

  def checks: JumioChecks

  def timestamp: Option[ZonedDateTime]

  def callbackTimestamp: Option[ZonedDateTime]

  override def merchantScanReference: String

  def customerId: Option[String]

  def clientIp: Option[String]

  def links: JumioScanLinks

  def rawData: Map[String, String]
}


object JumioScanResult extends JumioUnmarshaller {

  import OptionStringUtils._

  def of(parameters: Map[String, String]): JumioScanResult = {

    def scanReference = parameters.get("jumioIdScanReference").getOrUnknown

    def status = parameters.get("verificationStatus")
      .map(EnumJumioDocumentStatus.withName)
      .getOrElse(EnumJumioDocumentStatus.UNKNOWN)

    def source = parameters.get("idScanSource")
      .map(EnumJumioSources.withName)
      .getOrElse(EnumJumioSources.unknown)

    def checks = JumioChecks.of(parameters)

    def timestamp = parameters.get("transactionDate").toDateTime

    def callbackTimestamp = parameters.get("callbackDate").toDateTime

    def merchantIdScanReference = parameters.get("merchantIdScanReference").getOrUnknown

    def customerId = parameters.get("customerId")

    def clientIp = parameters.get("clientIp")

    def additionalInformation = parameters.get("additionalInformation").getOrUnknown

    def rejectReason = {
      parameters.get("rejectReason").map(_.parseJson.convertTo[JumioRejectReason])
        .getOrElse(JumioRejectReason("UNKNOWN", "UNKNOWN", Seq.empty))
    }

    parameters.getOrElse("idScanStatus", "ERROR") match {
      case "SUCCESS" => JumioScanSuccess(
        scanReference,
        status,
        source,
        checks,
        timestamp,
        callbackTimestamp,
        JumioDocument.of(parameters),
        merchantIdScanReference,
        customerId,
        clientIp,
        additionalInformation,
        JumioScanLinks.of(parameters),
        IdentityVerification.of(parameters),
        parameters
      )
      case "ERROR" =>
        JumioScanFailure(
          scanReference = scanReference,
          status = status,
          source = source,
          checks = checks,
          timestamp = timestamp,
          callbackTimestamp = callbackTimestamp,
          rejectReason = rejectReason,
          merchantScanReference = merchantIdScanReference,
          customerId = customerId,
          clientIp = clientIp,
          links = JumioScanLinks.of(parameters),
          rawData = parameters
        )
    }
  }
}