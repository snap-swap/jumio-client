package com.snapswap.jumio.model

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import com.snapswap.jumio.model.EnumJumioDocumentStatus.JumioDocumentStatus
import com.snapswap.jumio.model.JumioGenderEnum.Gender
import spray.json._


case class JumioDocument(`type`: Option[EnumJumioDocTypes.JumioDocType],
                         subType: Option[String],
                         issuingCountry: Option[String],
                         issueDate: Option[ZonedDateTime],
                         givenName: Option[String],
                         familyName: Option[String],
                         dob: Option[LocalDate],
                         expiry: Option[LocalDate],
                         number: Option[String],
                         gender: Option[JumioGenderEnum.Gender],
                         nationality: Option[String],
                         personalNumber: Option[String],
                         address: Option[JumioAddress],
                         extractedData: Option[JumioExtractedData],
                         status: Option[EnumJumioDocumentStatus.JumioDocumentStatus],
                         mrz: Option[JumioMrzData],
                         placeOfBirth: Option[String]) {
  override def toString: String = `type` match {
    case None => "N/A"
    case Some(t) =>
      (issuingCountry.map(v => s"$v ") :: Some(t.toString) ::
        subType.map(v => s"($v)") ::
        familyName.map(surname => " of '" + givenName.getOrElse("") + s"' '$surname'") ::
        dob.map(v => s", born '${v.format(JumioDocument.fomatter)}'") ::
        address.map(v => s" resident of '$v'") ::
        number.map(v => s", '$v' number") ::
        personalNumber.map(v => s", '$v' personal number") ::
        gender.map(v => s", '$v' gender") ::
        nationality.map(v => s", '$v' nationality") ::
        expiry.map(v => s", expiry at '${v.format(JumioDocument.fomatter)}'") ::
        mrz.map(m => s" , $m") ::
        Nil).flatten.mkString
  }
}

object JumioDocument {
  def of(parameters: Map[String, String], idIssueDate: Option[ZonedDateTime]): JumioDocument = {
    def idType: Option[EnumJumioDocTypes.Value] = parameters.get("idType").map(EnumJumioDocTypes.withName)

    def idSubtype: Option[String] = parameters.get("idSubtype")

    def idCountry: Option[String] = parameters.get("idCountry")

    def idNumber: Option[String] = parameters.get("idNumber")

    def idFirstName: Option[String] = parameters.get("idFirstName")

    def idLastName: Option[String] = parameters.get("idLastName")

    def idDob: Option[LocalDate] = parameters.get("idDob").map(localDate)

    def idExpiry: Option[LocalDate] = parameters.get("idExpiry").map(localDate)

    def personalNumber: Option[String] = parameters.get("personalNumber")

    def idAddress: Option[JumioAddress] = parameters.get("idAddress").map(_.parseJson.convertTo[JumioAddress])

    def extractedData: Option[JumioExtractedData] = parameters.get("extractedData").map(_.parseJson.convertTo[JumioExtractedData])

    def status: Option[JumioDocumentStatus] = parameters.get("status").map(EnumJumioDocumentStatus.withName)

    def gender: Option[Gender] = parameters.get("gender").flatMap(JumioGenderEnum.parse)

    def nationality: Option[String] = parameters.get("nationality")

    def mrz: Option[JumioMrzData] = {
      val data = Seq(parameters.get("optionalData1"), parameters.get("optionalData2")).flatten
      if (data.isEmpty) {
        None
      } else {
        Some(JumioMrzData(data))
      }
    }

    def placeOfBirth: Option[String] = parameters.get("placeOfBirth")

    JumioDocument(
      idType,
      idSubtype,
      idCountry,
      idIssueDate,
      idFirstName,
      idLastName,
      idDob,
      idExpiry,
      idNumber,
      gender,
      nationality,
      personalNumber,
      idAddress,
      extractedData,
      status,
      mrz,
      placeOfBirth
    )
  }

  private[model] val fomatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private[model] def localDate(`yyyy-MM-dd`: String): LocalDate = LocalDate.parse(`yyyy-MM-dd`, fomatter)
}
