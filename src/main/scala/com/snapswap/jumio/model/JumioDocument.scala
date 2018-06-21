package com.snapswap.jumio.model

import java.time.{LocalDate, ZonedDateTime}
import java.time.format.DateTimeFormatter

import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import spray.json._


case class JumioDocument(`type`: Option[EnumJumioDocTypes.JumioDocType],
                         subType: Option[String],
                         issuingCountry: Option[String],
                         issueDate: Option[ZonedDateTime],
                         givenName: Option[String],
                         familyName: Option[String],
                         dob: Option[String],
                         expiry: Option[String],
                         number: Option[String],
                         gender: Option[String],
                         nationality: Option[String],
                         personalNumber: Option[String],
                         address: Option[JumioAddress],
                         extractedData: Option[JumioExtractedData],
                         status: Option[EnumJumioDocumentStatus.JumioDocumentStatus]) {
  override def toString: String = `type` match {
    case None => "N/A"
    case Some(t) =>
      (issuingCountry.map(v => s"$v ") :: Some(t.toString) ::
        subType.map(v => s"($v)") ::
        familyName.map(surname => " of '" + givenName.getOrElse("") + s"' '$surname'") ::
        dob.map(v => s", born '$v'") ::
        address.map(v => s" resident of '$v'") ::
        number.map(v => s", '$v' number") ::
        personalNumber.map(v => s", '$v' personal number") ::
        gender.map(v => s", '$v' gender") ::
        nationality.map(v => s", '$v' nationality") ::
        expiry.map(v => s", expiry at '$v'") :: Nil).flatten.mkString
  }

  val birthdate: Option[LocalDate] = dob.map(JumioDocument.localDate)
  val expireAt: Option[LocalDate] = expiry.map(JumioDocument.localDate)
}

object JumioDocument {
  def of(parameters: Map[String, String], idIssueDate: Option[ZonedDateTime]): JumioDocument = {
    def idType = parameters.get("idType").map(EnumJumioDocTypes.withName)

    def idSubtype = parameters.get("idSubtype")

    def idCountry = parameters.get("idCountry")

    def idNumber = parameters.get("idNumber")

    def idFirstName = parameters.get("idFirstName")

    def idLastName = parameters.get("idLastName")

    def idDob = parameters.get("idDob")

    def idExpiry = parameters.get("idExpiry")

    def personalNumber = parameters.get("personalNumber")

    def idAddress = parameters.get("idAddress").map(_.parseJson.convertTo[JumioAddress])

    def extractedData = parameters.get("extractedData").map(_.parseJson.convertTo[JumioExtractedData])

    def status = parameters.get("status").map(_.parseJson.convertTo[EnumJumioDocumentStatus.JumioDocumentStatus])

    def gender = parameters.get("gender")

    def nationality = parameters.get("nationality")

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
      status
    )
  }

  private val fomatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  protected def localDate(`yyyy-MM-dd`: String): LocalDate = LocalDate.parse(`yyyy-MM-dd`, fomatter)
}
