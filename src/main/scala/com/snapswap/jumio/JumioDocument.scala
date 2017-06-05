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
  // Multi-Document types


  val bs = Value("BS") // Bank statement, front side
  val ic = Value("IC") // Insurance card, front side
  val ub = Value("UB") //  Utility bill, front side
  val caap = Value("CAAP") //  Cash advance application, front and back side
  val crc = Value("CRC") //  Corporate resolution certificate, front and back side
  val ccs = Value("CCS") //  Credit card statement, front and back side
  val lag = Value("LAG") //  Lease agreement, front and back side
  val loap = Value("LOAP") //  Loan application, front and back side
  val moap = Value("MOAP") //  Mortgage application, front and back side
  val tr = Value("TR") //  Tax return, front and back side
  val vt = Value("VT") //  Vehicle title, front side
  val vc = Value("VC") //  Voided check, front side
  val stuc = Value("STUC") //  Student card, front side
  val hcc = Value("HCC") //  Health care card, front side
  val cb = Value("CB") //  Council bill, front side
  val senc = Value("SENC") //  Seniors card, front side
  val medc = Value("MEDC") //  Medicare card, front side
  val bc = Value("BC") //  Birth certificate, front side
  val wwcc = Value("WWCC") //  Working with children check, front side
  val ss = Value("SS") //  Superannuation statement, front side
  val tac = Value("TAC") //  Trade association card, front side
  val sel = Value("SEL") //  School enrolment letter, front side
  val pb = Value("PB") //  Phone bill, front side
  val usss = Value("USSS") //  US social security card, front side
  val ssc = Value("SSC") //  Social security card, front side
  val custom = Value("CUSTOM") //  Custom document type
}

object EnumJumioMDDocumentStatus extends Enumeration {
  type JumioMDDocumentStatus = Value

  val UPLOADED = Value("UPLOADED")
  val EXTRACTED = Value("EXTRACTED")
  val DISCARDED = Value("DISCARDED")
}

case class JumioExtractedData(firstName: Option[String],
                              lastName: Option[String],
                              name: Option[String],
                              ssn: Option[String],
                              signatureAvailable: Option[Boolean],
                              accountNumber: Option[String],
                              issueDate: Option[String],
                              address: Option[JumioAddress])

case class JumioDocument(`type`: Option[EnumJumioDocTypes.JumioDocType],
                         subType: Option[String],
                         issuingCountry: Option[String],
                         givenName: Option[String],
                         familyName: Option[String],
                         dob: Option[String],
                         expiry: Option[String],
                         number: Option[String],
                         personalNumber: Option[String],
                         address: Option[JumioAddress],
                         extractedData: Option[JumioExtractedData],
                         status: Option[EnumJumioMDDocumentStatus.JumioMDDocumentStatus]) {
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

    def idAddress = parameters.get("idAddress").map(s =>
      com.snapswap.jumio.unmarshaller.jumioAddressReader.read(s.parseJson)
    )

    def extractedData = parameters.get("extractedData").map(s =>
      com.snapswap.jumio.unmarshaller.jumioExtractedDataReader.read(s.parseJson)
    )

    def status = parameters.get("status").map(s =>
      com.snapswap.jumio.unmarshaller.enumJumioMDDocumentStatusFormat.read(s.parseJson)
    )

    JumioDocument(
      idType, idSubtype, idCountry, idFirstName, idLastName, idDob, idExpiry,
      idNumber, personalNumber, idAddress, extractedData, status
    )
  }

  private val localDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  protected def localDate(`yyyy-MM-dd`: String): LocalDate = LocalDate.parse(`yyyy-MM-dd`, localDateFormat)
}

object EnumJumioAddressFormats extends Enumeration {
  type JumioAddressFormat = Value
  val us, eu, raw = Value
}

case class JumioAddress(country: String,
                        region: Option[String],
                        city: String,
                        postalCode: String,
                        streetAddress: String,
                        protected val format: EnumJumioAddressFormats.JumioAddressFormat) {
  override def toString = s"$streetAddress, $postalCode $city, $country"
}
