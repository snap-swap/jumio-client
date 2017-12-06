package com.snapswap.jumio.model.md

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.snapswap.jumio.model.{EnumJumioDocTypes, EnumJumioSources, JumioAddress}
import com.snapswap.jumio.utils.OptionObjUtils


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
    import OptionObjUtils._

    "SUCCESS multi document callback [" +
      s"scanReference=$scanReference" +
      s", firstName=${firstName.toStringOrNA}" +
      s", lastName=${lastName.toStringOrNA}" +
      s", name=${name.toStringOrNA}" +
      s", ssn=${ssn.toStringOrNA}" +
      s", signatureAvailable=${signatureAvailable.toStringOrNA}" +
      s", accountNumber=${accountNumber.toStringOrNA}" +
      s", issueDate=${issueDateRawFormat.toStringOrNA}" +
      s", address=[${address.toStringOrNA}]" +
      "]"
  }

  private val localDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private def localDate(`yyyy-MM-dd`: String): LocalDate = LocalDate.parse(`yyyy-MM-dd`, localDateFormat)

  val issueDate: Option[LocalDate] = issueDateRawFormat.map(localDate)
}
