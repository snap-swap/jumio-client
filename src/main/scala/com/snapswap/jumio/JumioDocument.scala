package com.snapswap.jumio

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import spray.json._

object EnumJumioDocTypes extends Enumeration {
  type JumioDocType = Value
  val passport = Value("PASSPORT")
  val driving_license = Value("DRIVING_LICENSE")
  val id_card = Value("ID_CARD")
  val unsupported = Value("UNSUPPORTED")
}

case class JumioDocument(`type`: Option[EnumJumioDocTypes.JumioDocType], subType: Option[String],
                         issuingCountry: Option[String],
                         givenName: Option[String], familyName: Option[String], dob: Option[String],
                         expiry: Option[String], number: Option[String], personalNumber: Option[String],
                         address: Option[JumioAddress]
                        ) {
  override def toString: String = `type` match {
    case None => "N/A"
    case Some(t) => (issuingCountry.map(v => s"$v ") :: Some(t.toString) ::
      subType.map(v => s"($v)") ::
      familyName.map(surname => " of '" + givenName.getOrElse("") + s"' '$surname'") ::
      dob.map(v => s", born '$v'") ::
      address.map(v => s" resident of '$v'") ::
      number.map(v => s", '$v' number") ::
      personalNumber.map(v => s", '$v' personal number") ::
      expiry.map(v => s", expiry at '$v'") :: Nil).flatten.mkString
  }

  val birthdate: Option[LocalDate] = dob.map(JumioDocument.localDate)
  val expireAt: Option[LocalDate] = expiry.map(JumioDocument.localDate)
}

object JumioDocument {
  def of(parameters: Map[String, String]): JumioDocument = {
    def idType = parameters.get("idType").map(EnumJumioDocTypes.withName)
    def idSubtype = parameters.get("idSubtype")
    def idCountry = parameters.get("idCountry")
    def idNumber = parameters.get("idNumber")
    def idFirstName = parameters.get("idFirstName")
    def idLastName = parameters.get("idLastName")
    def idDob = parameters.get("idDob")
    def idExpiry = parameters.get("idExpiry")
    def personalNumber = parameters.get("personalNumber")
    def idAddress = parameters.get("idAddress").map(s => com.snapswap.jumio.unmarshaller.jumioAddressReader.read(s.parseJson))
    JumioDocument(idType, idSubtype, idCountry, idFirstName, idLastName, idDob, idExpiry, idNumber, personalNumber, idAddress)
  }

  private val localDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  protected def localDate(`yyyy-MM-dd`: String): LocalDate = LocalDate.parse(`yyyy-MM-dd`, localDateFormat)
}

object EnumJumioAddressFormats extends Enumeration {
  type JumioAddressFormat = Value
  val us, eu, raw = Value
}

case class JumioAddress(country: String, region: Option[String], city: String, postalCode: String, streetAddress: String, protected val format: EnumJumioAddressFormats.JumioAddressFormat) {
  override def toString = s"$streetAddress, $postalCode $city, $country"
}
