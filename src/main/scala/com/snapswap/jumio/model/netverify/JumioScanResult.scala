package com.snapswap.jumio.model.netverify

import java.time.ZonedDateTime

import com.snapswap.jumio.json.protocol.JumioUnmarshaller
import com.snapswap.jumio.model.{EnumJumioDocumentStatus, EnumJumioSources, _}
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

    def scanReference: String = parameters.get("jumioIdScanReference").getOrUnknown

    def status: EnumJumioDocumentStatus.Value = parameters.get("verificationStatus")
      .map(EnumJumioDocumentStatus.withName)
      .getOrElse(EnumJumioDocumentStatus.UNKNOWN)

    def source: EnumJumioSources.Value = parameters.get("idScanSource")
      .map(EnumJumioSources.withName)
      .getOrElse(EnumJumioSources.unknown)

    def checks: JumioChecks = JumioChecks.of(parameters)

    def timestamp: Option[ZonedDateTime] = parameters.get("transactionDate").toDateTime

    def callbackTimestamp: Option[ZonedDateTime] = parameters.get("callbackDate").toDateTime

    def merchantIdScanReference: String = parameters.get("merchantIdScanReference").getOrUnknown

    def customerId: Option[String] = parameters.get("customerId")

    def clientIp: Option[String] = parameters.get("clientIp")

    def issuingDate: Option[ZonedDateTime] = parameters.get("issuingDate").toDate

    def additionalInformation: String = parameters.get("additionalInformation").getOrUnknown

    def rejectReason: JumioRejectReason = {
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
        JumioDocument.of(parameters, issuingDate),
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