package com.snapswap.jumio.callback

import org.joda.time.DateTime
import akka.http.scaladsl.model.Uri
import com.snapswap.jumio._
import spray.json._
import com.snapswap.jumio.unmarshaller._

case class JumioChecks(dataPositions: Boolean,
                       documentValidation: Boolean,
                       hologram: Boolean,
                       mrzCode: Boolean,
                       microprint: Boolean,
                       securityFeatures: Boolean,
                       signature: Boolean) {
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

    new JumioScanLinks(idScanImage, idScanImageBackside, idScanImageFace)
  }

  def of(str: String): JumioScanLinks = of(str.split(',').map(_.trim).flatMap { kv =>
    val arr = kv.split('=')
    val key = if (arr.length > 0) Some(arr(0)) else None
    val value = if (arr.length > 1) Some(arr(1)) else None
    key.flatMap(k => value.map(v => k -> v))
  }.toMap)
}

trait JumioScanResult extends JumioNetverifyResult{
  override def scanReference: String

  def status: EnumJumioVerificationStatuses.JumioVerificationStatus

  override def source: EnumJumioSources.JumioSource

  def checks: JumioChecks

  def timestamp: Option[DateTime]

  def callbackTimestamp: Option[DateTime]

  override def merchantScanReference: String

  def customerId: Option[String]

  def clientIp: Option[String]

  def links: JumioScanLinks
}

case class JumioScanSuccess(scanReference: String,
                            status: EnumJumioVerificationStatuses.JumioVerificationStatus,
                            source: EnumJumioSources.JumioSource,
                            checks: JumioChecks,
                            timestamp: Option[DateTime],
                            callbackTimestamp: Option[DateTime],
                            document: JumioDocument,
                            faceMatch: Option[Int],
                            faceLiveness: Option[Boolean],
                            merchantScanReference: String,
                            customerId: Option[String],
                            clientIp: Option[String],
                            additionalInformation: String,
                            links: JumioScanLinks) extends JumioScanResult {
  override def toString: String = {
    import RichToString._

    def faceDescr = faceMatch.map(p => p + "% faceMatch (looks " + (if (faceLiveness.getOrElse(false)) "alive" else "dead") + ")").getOrElse("faceMatch not performed")

    s"$status $scanReference (merchantScanID=$merchantScanReference, customerID=${customerId.orUnknown}) " +
      s"from $source at IP ${clientIp.orUnknown}, scanned at ${timestamp.orUnknown}, completed at ${callbackTimestamp.orUnknown}, " +
      s"$checks, $faceDescr, URLs [$links]: $document"
  }
}

case class JumioScanFailure(scanReference: String,
                            status: EnumJumioVerificationStatuses.JumioVerificationStatus,
                            source: EnumJumioSources.JumioSource, checks: JumioChecks,
                            timestamp: Option[DateTime],
                            callbackTimestamp: Option[DateTime],
                            rejectReason: JumioRejectReason,
                            merchantScanReference: String,
                            customerId: Option[String],
                            clientIp: Option[String],
                            links: JumioScanLinks) extends JumioScanResult with JumioNetverifyFailure {
  override def toString: String = {
    import RichToString._

    s"$status $scanReference (merchantScanID=$merchantScanReference, customerID=${customerId.orUnknown}) " +
      s"from $source at IP ${clientIp.orUnknown}, scanned at ${timestamp.orUnknown}, completed at ${callbackTimestamp.orUnknown}, " +
      s"$checks, URLs [$links]: rejected as $rejectReason"
  }

  override def error = status.toString

  override def details = rejectReason.toString
}

object JumioScanResult {

  def of(parameters: Map[String, String]): JumioScanResult = {
    import Extractors._

    def scanReference = parameters.getOrUnknown("jumioIdScanReference")

    def status = EnumJumioVerificationStatuses.withName(parameters.getOrUnknown("verificationStatus"))

    def source = EnumJumioSources.withName(parameters.getOrUnknown("idScanSource"))

    def checks = JumioChecks.of(parameters)

    def timestamp = parameters.getTimestamp("transactionDate")

    def callbackTimestamp = parameters.getTimestamp("callbackDate")

    def idFaceMatch = parameters.get("idFaceMatch").map(_.toInt)

    def idFaceLiveness = parameters.get("idFaceLiveness").map(_.equalsIgnoreCase("true"))

    def merchantIdScanReference = parameters.getOrUnknown("merchantIdScanReference")

    def customerId = parameters.getString("customerId")

    def clientIp = parameters.getString("clientIp")

    def additionalInformation = parameters.getOrUnknown("additionalInformation")

    def rejectReason = {
      parameters.get("rejectReason").map(_.parseJson.convertTo[JumioRejectReason])
        .getOrElse(JumioRejectReason("UNKNOWN", "UNKNOWN", Seq.empty))
    }

    parameters.getOrElse("idScanStatus", "ERROR") match {
      case "SUCCESS" => JumioScanSuccess(scanReference, status, source, checks,
        timestamp, callbackTimestamp,
        JumioDocument.of(parameters),
        idFaceMatch, idFaceLiveness,
        merchantIdScanReference, customerId, clientIp,
        additionalInformation, JumioScanLinks.of(parameters)
      )
      case "ERROR" => JumioScanFailure(scanReference, status, source, checks, timestamp, callbackTimestamp, rejectReason,
        merchantIdScanReference, customerId, clientIp, JumioScanLinks.of(parameters))
    }
  }
}
