package com.snapswap.jumio

import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

object EnumJumioDocumentStatus extends Enumeration {
  type JumioDocumentStatus = Value

  //from md callback
  val UPLOADED = Value("UPLOADED")
  val EXTRACTED = Value("EXTRACTED")
  val DISCARDED = Value("DISCARDED")

  //from retrieval api
  val NO_ID_UPLOADED = Value("NO_ID_UPLOADED")
  val DOCUMENT_PRESENT = Value("DOCUMENT_PRESENT")
  val NO_DOCUMENT_PRESENT = Value("NO_DOCUMENT_PRESENT")
  val DENIED_UNSUPPORTED_DOCUMENT_TYPE = Value("DENIED_UNSUPPORTED_DOCUMENT_TYPE")
  val APPROVED_VERIFIED = Value("APPROVED_VERIFIED")
  val DENIED_FRAUD = Value("DENIED_FRAUD")
  val DENIED_UNSUPPORTED_ID_TYPE = Value("DENIED_UNSUPPORTED_ID_TYPE")
  val DENIED_UNSUPPORTED_ID_COUNTRY = Value("DENIED_UNSUPPORTED_ID_COUNTRY")
  val DENIED_NAME_MISMATCH = Value("DENIED_NAME_MISMATCH")
  val ERROR_NOT_READABLE_ID = Value("ERROR_NOT_READABLE_ID")
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
      com.snapswap.jumio.unmarshaller.enumJumioDocumentStatusFormat.read(s.parseJson)
    )

    JumioDocument(
      idType, idSubtype, idCountry, idFirstName, idLastName, idDob, idExpiry,
      idNumber, personalNumber, idAddress, extractedData, status
    )
  }

  private val fomatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  protected def localDate(`yyyy-MM-dd`: String): LocalDate = LocalDate.parse(`yyyy-MM-dd`, fomatter)
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
