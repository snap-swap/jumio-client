package com.snapswap.jumio.callback

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.snapswap.jumio._
import com.snapswap.jumio.unmarshaller._
import spray.json._

trait JumioMDScanResult extends JumioNetverifyResult {

  override def merchantScanReference: String

  override def scanReference: String

  override def source: EnumJumioSources.JumioSource

  def firstName: Option[String]

  def lastName: Option[String]

  def name: Option[String]

  def ssn: Option[String]

  def signatureAvailable: Option[Boolean]

  def accountNumber: Option[String]

  def issueDateRawFormat: Option[String]

  def address: Option[JumioAddress]
}

case class JumioMDScanSuccess(docType: EnumJumioDocTypes.JumioDocType,
                              merchantScanReference: String,
                              scanReference: String,
                              source: EnumJumioSources.JumioSource,
                              firstName: Option[String],
                              lastName: Option[String],
                              name: Option[String],
                              ssn: Option[String],
                              signatureAvailable: Option[Boolean],
                              accountNumber: Option[String],
                              issueDateRawFormat: Option[String],
                              address: Option[JumioAddress]) extends JumioMDScanResult {
  override def toString: String = {
    import RichToString._

    "SUCCESS multi document callback [" +
      s"scanReference=$scanReference" +
      s", firstName=${firstName.orUnknown}" +
      s", lastName=${lastName.orUnknown}" +
      s", name=${name.orUnknown}" +
      s", ssn=${ssn.orUnknown}" +
      s", signatureAvailable=${signatureAvailable.orUnknown}" +
      s", accountNumber=${accountNumber.orUnknown}" +
      s", issueDate=${issueDateRawFormat.orUnknown}" +
      s", address=[${address.orUnknown}]" +
      "]"
  }

  private val localDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private def localDate(`yyyy-MM-dd`: String): LocalDate = LocalDate.parse(`yyyy-MM-dd`, localDateFormat)

  val issueDate: Option[LocalDate] = issueDateRawFormat.map(localDate)
}

case class JumioMDScanFailure(merchantScanReference: String,
                              scanReference: String,
                              source: EnumJumioSources.JumioSource,
                              override val error: String,
                              override val details: String) extends JumioMDScanResult with JumioNetverifyFailure {
  val firstName: Option[String] = None
  val lastName: Option[String] = None
  val name: Option[String] = None
  val ssn: Option[String] = None
  val signatureAvailable: Option[Boolean] = None
  val accountNumber: Option[String] = None
  val issueDateRawFormat: Option[String] = None
  val address: Option[JumioAddress] = None

  override def toString: String = {
    s"FAILURE multi document callback scanReference=$scanReference] with reason [$details]"
  }
}

object JumioMDScanResult {
  def of(parameters: Map[String, String]): JumioMDScanResult = {
    parseJumioTx(parameters) match {
      case Some(JumioTx(status, source, _, _, _, _, Some(merchantScanReference), _)) if status == EnumJumioTxStatuses.done =>
        parseDocument(parameters) match {
          case Some(JumioDocument(docType, _, _, _, _, _, _, _, _, _, extractedData, Some(docStatus)))
            if docStatus == EnumJumioDocumentStatus.EXTRACTED || docStatus == EnumJumioDocumentStatus.UPLOADED =>
            JumioMDScanSuccess(
              docType = docType.getOrElse(throw new RuntimeException(s"document type must be in provided")),
              merchantScanReference = merchantScanReference,
              scanReference = parseScanReference(parameters),
              source = source.getOrElse(EnumJumioSources.unknown),
              firstName = extractedData.flatMap(_.firstName),
              lastName = extractedData.flatMap(_.lastName),
              name = extractedData.flatMap(_.name),
              ssn = extractedData.flatMap(_.ssn),
              signatureAvailable = extractedData.flatMap(_.signatureAvailable),
              accountNumber = extractedData.flatMap(_.accountNumber),
              issueDateRawFormat = extractedData.flatMap(_.issueDate),
              address = extractedData.flatMap(_.address)
            )
          case Some(JumioDocument(_, _, _, _, _, _, _, _, _, _, _, Some(docStatus)))
            if docStatus == EnumJumioDocumentStatus.DISCARDED =>
            JumioMDScanFailure(
              merchantScanReference = merchantScanReference,
              scanReference = parseScanReference(parameters),
              source = source.getOrElse(EnumJumioSources.unknown),
              error = docStatus.toString,
              s"Expected transaction status is 'EXTRACTED' or 'UPLOADED' but found 'DISCARDED'"
            )
          case other =>
            JumioMDScanFailure(
              merchantScanReference = merchantScanReference,
              scanReference = parseScanReference(parameters),
              source = source.getOrElse(EnumJumioSources.unknown),
              error = "Unknown callback format",
              s"Unknown callback format: '$other'"
            )
        }
      case Some(JumioTx(status, source, _, _, _, _, Some(merchantScanReference), _)) if status != EnumJumioTxStatuses.done =>
        JumioMDScanFailure(
          merchantScanReference = merchantScanReference,
          scanReference = parseScanReference(parameters),
          source = source.getOrElse(EnumJumioSources.unknown),
          error = status.toString,
          s"Expected transaction status is 'done' but found '$status'"
        )
      case Some(JumioTx(_, source, _, _, _, _, None, _)) =>
        JumioMDScanFailure(
          merchantScanReference = "N/A",
          scanReference = parseScanReference(parameters),
          source = source.getOrElse(EnumJumioSources.unknown),
          error = "merchantScanReference must be presented",
          s"merchantScanReference must be presented"
        )
      case None =>
        throw new RuntimeException(s"JumioTx must be in callback")
    }
  }

  private def parseScanReference(parameters: Map[String, String]): String = {
    parameters.getOrElse("scanReference", throw new RuntimeException(s"scanReference must be in callback"))
  }

  private def parseDocument(parameters: Map[String, String]): Option[JumioDocument] = {
    parameters
      .get("document")
      .map(_.parseJson.convertTo[JumioDocument])
  }

  private def parseJumioTx(parameters: Map[String, String]): Option[JumioTx] = {
    parameters
      .get("transaction")
      .map(_.parseJson.convertTo[JumioTx])
  }
}