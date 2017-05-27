package com.snapswap.jumio.callback

import com.snapswap.jumio._


trait JumioMDScanResult {
  def scanReference: String

  def firstName: Option[String]

  def lastName: Option[String]

  def name: Option[String]

  def ssn: Option[String]

  def signatureAvailable: Option[Boolean]

  def accountNumber: Option[String]

  def issueDate: Option[String]

  def address: Option[JumioAddress]
}

case class JumioMDScanSuccess(scanReference: String,
                              firstName: Option[String],
                              lastName: Option[String],
                              name: Option[String],
                              ssn: Option[String],
                              signatureAvailable: Option[Boolean],
                              accountNumber: Option[String],
                              issueDate: Option[String],
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
      s", issueDate=${issueDate.orUnknown}" +
      s", address=[${address.orUnknown}]" +
      "]"
  }
}

case class JumioMDScanFailure(scanReference: String,
                              reason: String) extends JumioMDScanResult {
  val firstName: Option[String] = None
  val lastName: Option[String] = None
  val name: Option[String] = None
  val ssn: Option[String] = None
  val signatureAvailable: Option[Boolean] = None
  val accountNumber: Option[String] = None
  val issueDate: Option[String] = None
  val address: Option[JumioAddress] = None

  override def toString: String = {
    s"FAILURE multi document callback scanReference=$scanReference] with reason [$reason]"
  }
}

object JumioMDScanResult {
  def of(parameters: Map[String, String]): JumioMDScanResult = {
    parseTxStatus(parameters) match {
      case EnumJumioTxStatuses.done =>
        parseDocument(parameters) match {
          case Some(JumioDocument(_, _, _, _, _, _, _, _, _, _, extractedData, Some(status))) if status == EnumJumioMDDocumentStatus.EXTRACTED || status == EnumJumioMDDocumentStatus.UPLOADED =>
            JumioMDScanSuccess(
              scanReference = parseScanReference(parameters),
              firstName = extractedData.flatMap(_.firstName),
              lastName = extractedData.flatMap(_.lastName),
              name = extractedData.flatMap(_.name),
              ssn = extractedData.flatMap(_.ssn),
              signatureAvailable = extractedData.flatMap(_.signatureAvailable),
              accountNumber = extractedData.flatMap(_.accountNumber),
              issueDate = extractedData.flatMap(_.issueDate),
              address = extractedData.flatMap(_.address)
            )
          case Some(JumioDocument(_, _, _, _, _, _, _, _, _, _, extractedData, Some(status))) if status == EnumJumioMDDocumentStatus.DISCARDED =>
            JumioMDScanFailure(parseScanReference(parameters), s"Expected transaction status is 'EXTRACTED' or 'UPLOADED' but found 'DISCARDED'")
          case other =>
            JumioMDScanFailure(parseScanReference(parameters), s"Unknown callback format '$other'")
        }

      case other =>
        JumioMDScanFailure(parseScanReference(parameters), s"Expected transaction status is 'done' but found '$other'")
    }
  }

  private def parseScanReference(parameters: Map[String, String]): String = {
    parameters.getOrElse("scanReference", throw new RuntimeException(s"scanReference must be in callback"))
  }

  private def parseTxStatus(parameters: Map[String, String]): EnumJumioTxStatuses.JumioTxStatus = {
    import spray.json._
    import com.snapswap.jumio.unmarshaller._

    parameters
      .get("transaction")
      .map(_.parseJson.convertTo[JumioTx].status)
      .getOrElse(EnumJumioTxStatuses.failed)
  }

  private def parseDocument(parameters: Map[String, String]): Option[JumioDocument] = {
    import spray.json._
    import com.snapswap.jumio.unmarshaller._

    parameters
      .get("document")
      .map(_.parseJson.convertTo[JumioDocument])
  }
}