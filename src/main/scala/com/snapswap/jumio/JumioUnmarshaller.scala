package com.snapswap.jumio

import scala.util.{Try, Success, Failure}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json._

trait JumioUnmarshaller extends DefaultJsonProtocol {

  protected def enumNameFormat(enum: Enumeration) = new RootJsonFormat[enum.Value] {
    def read(value: JsValue) = value match {
      case JsString(s) => enum.withName(s)
      case x => deserializationError("Expected Enum as JsString, but got " + x)
    }
    def write(v: enum.Value) = JsString(v.toString)
  }

  implicit val dateTimeFormat = new RootJsonFormat[DateTime] {
    private val df = ISODateTimeFormat.date().withZoneUTC()
    private val dtf = ISODateTimeFormat.dateTime().withZoneUTC()
    override def read(json: JsValue) = json match {
      case JsString(str) if str.contains("T") =>
        Try(dtf.parseDateTime(str)) match {
          case Success(dt) => dt
          case Failure(ex) => deserializationError(s"Expected DateTime as JsString in ISO 8601 'yyyy-MM-ddTHH:mm:ss.SSSZ' format, but got '$str'", cause = ex)
        }
      case JsString(str) =>
        Try(df.parseDateTime(str)) match {
          case Success(dt) => dt
          case Failure(ex) => deserializationError(s"Expected Date as JsString in 'yyyy-MM-dd' format, but got '$str'", cause = ex)
        }
      case x => deserializationError("Expected DateTime as JsString, but got " + x)
    }
    override def write(obj: DateTime) = JsString(dtf.print(obj))
  }

  implicit val enumJumioTxStatusFormat = enumNameFormat(EnumJumioTxStatuses)
  implicit val enumJumioSourceFormat = enumNameFormat(EnumJumioSources)
  implicit val enumMRZCheckFormat = enumNameFormat(EnumJumioMRZCheck)
  implicit val enumJumioDocTypeFormat = enumNameFormat(EnumJumioDocTypes)

  implicit val jumioScanStatusFormat = jsonFormat3(JumioScanStatus)

  implicit val jumioTxFormat = jsonFormat8(JumioTx)

  implicit val jumioRejectionFormat = jsonFormat(JumioRejection, "detailsCode", "detailsDescription")
  implicit val jumioRejectReasonFormat = jsonFormat(JumioRejectReason, "rejectReasonCode", "rejectReasonDescription", "rejectReasonDetails")
  implicit val jumioVerificationFormat = jsonFormat(JumioVerification, "mrzCheck", "faceMatch", "rejectReason")

  protected trait Address {
    def toAddress: JumioAddress
  }

  protected case class USAddress(city: String, stateCode: Option[String],
                                    streetName: String, streetSuffix: Option[String], streetDirection: Option[String],
                                    streetNumber: Option[String], unitDesignator: Option[String], unitNumber: Option[String],
                                    zip: String, zipExtension: Option[String], country: String
                                   ) extends Address {
    def toAddress = JumioAddress(country = country, region = stateCode, city = city,
      postalCode = zip + zipExtension.map(v => s"-$v").getOrElse(""),
      streetAddress = (Some(streetName) ::
        streetSuffix.map(v => s" $v") ::
        streetDirection.map(v => s" $v") ::
        streetNumber.map(v => s" $v") ::
        unitNumber.map(num => ", " + unitDesignator.map(v => s"$v ").getOrElse("") + num) ::
        Nil).flatten.mkString,
      format = EnumJumioAddressFormats.us)
  }
  protected implicit val usAddressFormat = jsonFormat11(USAddress)

  protected case class EUAddress(city: String, province: Option[String], streetName: String,
                                 streetNumber: Option[String], unitDetails: Option[String],
                                 postalCode: String, country: String) extends Address {
    def toAddress = JumioAddress(country = country, region = province, city = city, postalCode = postalCode,
      streetAddress = (Some(streetName) ::
        streetNumber.map(v => s" $v") ::
        unitDetails.map(v => s", $v") ::
        Nil).flatten.mkString,
      format = EnumJumioAddressFormats.eu)
  }
  protected implicit val euAddressFormat = jsonFormat7(EUAddress)

  protected case class RawAddress(line1: String,
                                  line2: Option[String], line3: Option[String], line4: Option[String], line5: Option[String],
                                  country: String, postalCode: String, city: String) extends Address {
    def toAddress = JumioAddress(country = country,
      region = None,
      city = city, postalCode = postalCode,
      streetAddress = (Some(line1) :: line2 :: line3 :: line4 :: line5 ::
        Nil).flatten.map(_.trim).mkString(" "),
      format = EnumJumioAddressFormats.raw)
  }
  protected implicit val rawAddressFormat = jsonFormat8(RawAddress)

  implicit val jumioAddressReader = new RootJsonFormat[JumioAddress] {
    override def read(json: JsValue): JumioAddress = json match {
      case obj: JsObject =>
        (obj.fields.get("stateCode"), obj.fields.get("line1")) match {
          case (Some(_), None) => usAddressFormat.read(json).toAddress
          case (None, Some(_)) => rawAddressFormat.read(json).toAddress
          case _ => euAddressFormat.read(json).toAddress
        }
      case x => deserializationError("Expected address as object, but got " + x)
    }

    override def write(obj: JumioAddress): JsValue = throw new UnsupportedOperationException("Conversion of JumioAddress to JSON isn't supported")
  }

  implicit val jumioDocumentReader = jsonFormat(JumioDocument.apply, "type", "subType", "issuingCountry", "firstName", "lastName", "dob", "expiry", "number", "personalNumber", "address")

  implicit val jumioScanReader = jsonFormat5(JumioScan)
}

object unmarshaller extends JumioUnmarshaller
