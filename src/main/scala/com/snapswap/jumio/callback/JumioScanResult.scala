package com.snapswap.jumio.callback

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import akka.http.scaladsl.model.Uri
import com.snapswap.jumio._

case class JumioChecks(dataPositions: Boolean, documentValidation: Boolean, hologram: Boolean, mrzCode: Boolean,
                       microprint: Boolean, securityFeatures: Boolean, signature: Boolean) {
  private def okOrNA(check: Boolean) = if (check) "OK" else "N/A"
  override def toString: String = (
    "dataPositions=" + okOrNA(dataPositions) ::
      "documentValidation=" + okOrNA(documentValidation) ::
      "hologram=" + okOrNA(hologram) ::
      "mrzCode=" + okOrNA(mrzCode) ::
      "microprint=" + okOrNA(microprint) ::
      "securityFeatures=" + okOrNA(securityFeatures) ::
      "signature=" + okOrNA(signature) ::
      Nil).mkString("checked [", ", ", "]")
}

object JumioChecks {

  def of(parameters: Map[String, String]): JumioChecks = {

    def isOk(name: String) = parameters.getOrElse(name, "") == "OK"

    JumioChecks(
      dataPositions = isOk("idCheckDataPositions"),
      documentValidation = isOk("idCheckDocumentValidation"),
      hologram = isOk("idCheckHologram"),
      mrzCode = isOk("idCheckMRZcode"),
      microprint = isOk("idCheckMicroprint"),
      securityFeatures = isOk("idCheckSecurityFeatures"),
      signature = isOk("idCheckSignature")
    )
  }
}

case class JumioScanLinks(frontImage: Option[Uri], backImage: Option[Uri], faceImage: Option[Uri]) {
  override def toString: String = {
    def urlOrNA(uri: Option[Uri]) = uri.map(_.toString()).getOrElse("N/A")
    ("idScanImage=" + urlOrNA(frontImage) ::
      "idScanImageBackside=" + urlOrNA(backImage) ::
      "idScanImageFace=" + urlOrNA(faceImage) ::
      Nil).mkString(", ")
  }
}

object JumioScanLinks {
  def of(parameters: Map[String, String]): JumioScanLinks = {
    def getUri(name: String): Option[Uri] = parameters.get(name).map(Uri.apply)
    val idScanImage = getUri("idScanImage")
    val idScanImageBackside = getUri("idScanImageBackside")
    val idScanImageFace = getUri("idScanImageFace")

    JumioScanLinks(idScanImage, idScanImageBackside, idScanImageFace)
  }
  def of(str: String): JumioScanLinks = of(str.split(',').map(_.trim).flatMap { kv =>
    val arr = kv.split('=')
    val key = if (arr.length > 0) Some(arr(0)) else None
    val value = if (arr.length > 1) Some(arr(1)) else None
    key.flatMap(k => value.map(v => k -> v))
  }.toMap)
}

trait JumioScanResult {
  def scanReference: String
  def status: EnumJumioVerificationStatuses.JumioVerificationStatus
  def source: EnumJumioSources.JumioSource
  def checks: JumioChecks
  def timestamp: Option[DateTime]
  def callbackTimestamp: Option[DateTime]
}

case class JumioScanSuccess(scanReference: String, status: EnumJumioVerificationStatuses.JumioVerificationStatus,
                            source: EnumJumioSources.JumioSource, checks: JumioChecks,
                            timestamp: Option[DateTime], callbackTimestamp: Option[DateTime],
                            document: JumioDocument, faceMatch: Option[Int], faceLiveness: Option[Boolean],
                            merchantScanReference: String, customerId: String,
                            clientIp: String, additionalInformation: String, links: JumioScanLinks) extends JumioScanResult {
  override def toString: String = {
    def orUnknown(opt: Option[DateTime]) = opt.map(_.toString).getOrElse("UNKNOWN")
    def faceDescr = faceMatch.map(p => p + "% faceMatch (looks " + (if (faceLiveness.getOrElse(false)) "alive" else "dead") + ")").getOrElse("faceMatch not performed")
    s"$status $scanReference (merchantScanID=$merchantScanReference, customerID=$customerId) " +
      s"from $source at IP $clientIp, scanned at ${orUnknown(timestamp)}, completed at ${orUnknown(callbackTimestamp)}, " +
      s"$checks, $faceDescr, URLs [$links]: $document"
  }
}

case class JumioScanFailure(scanReference: String, status: EnumJumioVerificationStatuses.JumioVerificationStatus,
                            source: EnumJumioSources.JumioSource, checks: JumioChecks,
                            timestamp: Option[DateTime], callbackTimestamp: Option[DateTime],
                            rejectReason: JumioRejectReason,
                            merchantScanReference: Option[String], customerId: Option[String], clientIp: Option[String],
                            links: JumioScanLinks) extends JumioScanResult {
  override def toString: String = {
    def orUnknown(opt: Option[String]) = opt.getOrElse("UNKNOWN")
    s"$status $scanReference (merchantScanID=${orUnknown(merchantScanReference)}, customerID=${orUnknown(customerId)}) " +
      s"from $source at IP ${orUnknown(clientIp)}, scanned at ${orUnknown(timestamp.map(_.toString))}, completed at ${orUnknown(callbackTimestamp.map(_.toString))}, " +
      s"$checks, URLs [$links]: rejected as $rejectReason"
  }
}

object JumioScanResult {

  def of(parameters: Map[String, String]): JumioScanResult = {
    def getString(name: String) = parameters.get(name)
    val defaultString = "UNKNOWN"
    def getOrUnknown(name: String) = getString(name).getOrElse(defaultString)
    def getTimestamp(name: String) = parameters.get(name).map(ISODateTimeFormat.dateTime().withZoneUTC().parseDateTime)

    def scanReference = getOrUnknown("jumioIdScanReference")
    def status = EnumJumioVerificationStatuses.withName(getOrUnknown("verificationStatus"))
    def source = EnumJumioSources.withName(getOrUnknown("idScanSource"))
    def checks = JumioChecks.of(parameters)
    def timestamp = getTimestamp("transactionDate")
    def callbackTimestamp = getTimestamp("callbackDate")

    def idFaceMatch = parameters.get("idFaceMatch").map(_.toInt)
    def idFaceLiveness = parameters.get("idFaceLiveness").map(_.equalsIgnoreCase("true"))
    def merchantIdScanReference = getString("merchantIdScanReference")
    def customerId = getString("customerId")
    def clientIp = getString("clientIp")
    def additionalInformation = getOrUnknown("additionalInformation")

    def rejectReason = {
      import spray.json._
      import com.snapswap.jumio.unmarshaller._
      parameters.get("rejectReason").map(_.parseJson.convertTo[JumioRejectReason])
        .getOrElse(JumioRejectReason("UNKNOWN", "UNKNOWN", Seq()))
    }

    parameters.getOrElse("idScanStatus", "ERROR") match {
      case "SUCCESS" => JumioScanSuccess(scanReference, status, source, checks,
        timestamp, callbackTimestamp,
        JumioDocument.of(parameters),
        idFaceMatch, idFaceLiveness,
        merchantIdScanReference.getOrElse(defaultString), customerId.getOrElse(defaultString), clientIp.getOrElse(defaultString),
        additionalInformation, JumioScanLinks.of(parameters)
      )
      case "ERROR" => JumioScanFailure(scanReference, status, source, checks, timestamp, callbackTimestamp, rejectReason,
        merchantIdScanReference, customerId, clientIp, JumioScanLinks.of(parameters))
    }
  }
}
