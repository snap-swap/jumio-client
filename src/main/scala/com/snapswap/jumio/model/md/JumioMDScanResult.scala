package com.snapswap.jumio.model.md

import java.time.format.DateTimeFormatter

import com.snapswap.jumio.json.protocol.JumioUnmarshaller
import com.snapswap.jumio.model.{JumioResult, _}
import spray.json._

trait JumioMDScanResult extends JumioResult {

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

object JumioMDScanResult extends JumioUnmarshaller {
  def of(parameters: Map[String, String]): JumioMDScanResult = {
    parseJumioTx(parameters) match {
      case Some(JumioTx(status, source, _, _, _, _, Some(merchantScanReference), _)) if status == EnumJumioTxStatuses.done =>
        parseDocument(parameters) match {
          case Some(JumioDocument(docType, _, _, _, _, _, _, _, _, _, _, _, _, extractedData, Some(docStatus), _, _))
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
              issueDateRawFormat = extractedData.flatMap(_.issueDate.map(_.format(DateTimeFormatter.ISO_DATE))),
              address = extractedData.flatMap(_.address),
              rawData = parameters
            )
          case Some(JumioDocument(_, _, _, _, _, _, _, _, _, _,_, _, _, _, Some(docStatus), _, _))
            if docStatus == EnumJumioDocumentStatus.DISCARDED =>
            JumioMDScanFailure(
              merchantScanReference = merchantScanReference,
              scanReference = parseScanReference(parameters),
              source = source.getOrElse(EnumJumioSources.unknown),
              error = docStatus.toString,
              s"Expected transaction status is 'EXTRACTED' or 'UPLOADED' but found 'DISCARDED'",
              parameters
            )
          case other =>
            JumioMDScanFailure(
              merchantScanReference = merchantScanReference,
              scanReference = parseScanReference(parameters),
              source = source.getOrElse(EnumJumioSources.unknown),
              error = "Unknown callback format",
              s"Unknown callback format: '$other'",
              parameters
            )
        }
      case Some(JumioTx(status, source, _, _, _, _, Some(merchantScanReference), _)) if status != EnumJumioTxStatuses.done =>
        JumioMDScanFailure(
          merchantScanReference = merchantScanReference,
          scanReference = parseScanReference(parameters),
          source = source.getOrElse(EnumJumioSources.unknown),
          error = status.toString,
          s"Expected transaction status is 'done' but found '$status'",
          parameters
        )
      case Some(JumioTx(_, source, _, _, _, _, None, _)) =>
        JumioMDScanFailure(
          merchantScanReference = "N/A",
          scanReference = parseScanReference(parameters),
          source = source.getOrElse(EnumJumioSources.unknown),
          error = "merchantScanReference must be presented",
          s"merchantScanReference must be presented",
          parameters
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