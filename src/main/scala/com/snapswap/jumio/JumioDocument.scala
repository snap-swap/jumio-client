package com.snapswap.jumio

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object EnumJumioDocTypes extends Enumeration {
  type JumioDocType = Value
  val passport = Value("PASSPORT")
  val driving_license = Value("DRIVING_LICENSE")
  val id_card = Value("ID_CARD")
  val unsupported = Value("UNSUPPORTED")
}

case class JumioDocument(`type`: Option[EnumJumioDocTypes.JumioDocType],
                         issuingCountry: Option[String],
                         givenName: Option[String], familyName: Option[String], dob: Option[String],
                         expiry: Option[String], number: Option[String], address: Option[JumioAddress]
                        ) {
  override def toString = `type` match {
    case None => "N/A"
    case Some(t) => (issuingCountry.map(v => s"$v ") :: Some(t.toString) ::
      familyName.map(surname => " of '" + givenName.getOrElse("") + s"' '$surname'") ::
      dob.map(v => s", born $v") ::
      address.map(v => s" resident of '$v'") ::
      number.map(v => s", '$v' number") ::
      expiry.map(v => s", '$v' expiry") :: Nil).flatten.mkString
  }

  val birthdate: Option[DateTime] = dob.map(ISODateTimeFormat.date().withZoneUTC().parseDateTime)
  val expireAt: Option[DateTime] = expiry.map(ISODateTimeFormat.date().withZoneUTC().parseDateTime)
}

object EnumJumioAddressFormats extends Enumeration {
  type JumioAddressFormat = Value
  val us, eu, raw = Value
}

case class JumioAddress(country: String, region: Option[String], city: String, postalCode: String, streetAddress: String, protected val format: EnumJumioAddressFormats.JumioAddressFormat) {
  override def toString = s"$streetAddress, $postalCode $city, $country"
}
