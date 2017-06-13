package com.snapswap.jumio

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json._

import scala.util.{Failure, Success, Try}

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
  implicit val enumJumioDocumentStatusFormat = enumNameFormat(EnumJumioDocumentStatus)
  implicit val enumJumioImageMaskHintFormat = enumNameFormat(EnumJumioImageMaskHint)

  implicit val jumioScanStatusFormat = jsonFormat3(JumioScanStatus)

  implicit val jumioTxFormat = jsonFormat(JumioTx, "status", "source", "date", "clientIp", "customerId", "additionalInformation", "merchantScanReference", "merchantReportingCriteria")

  implicit val jumioRejectionFormat = jsonFormat(JumioRejection, "detailsCode", "detailsDescription")

  implicit val jumioRejectReasonFormat = new RootJsonFormat[JumioRejectReason] {
    override def read(json: JsValue): JumioRejectReason = {
      val f = json.asJsObject.fields

      val code =
        f.get("rejectReasonCode")
          .map(_.convertTo[String])
          .getOrElse(throw JumioMalformedResponse(json.prettyPrint))

      val description =
        f.get("rejectReasonDescription")
          .map(_.convertTo[String])
          .getOrElse(throw JumioMalformedResponse(json.prettyPrint))

      val details = f.get("rejectReasonDetails") match {
        case Some(JsArray(d)) =>
          d.map(_.convertTo[JumioRejection])
        case Some(d) =>
          Seq(d.convertTo[JumioRejection])
        case None =>
          Seq.empty
      }

      JumioRejectReason(code = code, description = description, details = details)
    }

    override def write(obj: JumioRejectReason): JsValue =
      SerializationUnsupported(obj)
  }

  implicit val jumioVerificationFormat = new RootJsonFormat[JumioVerification] {
    override def read(json: JsValue): JumioVerification = json match {
      case obj: JsObject =>
        val fields = obj.fields

        JumioVerification(
          fields.get("mrzCheck").map(_.convertTo[EnumJumioMRZCheck.JumioMRZCheck]),
          fields.get("faceMatch").map(_.convertTo[String].toInt),
          fields.get("rejectReason").map(_.convertTo[JumioRejectReason])
        )
      case x => deserializationError("Expected JumioVerification as object, but got " + x)
    }

    override def write(obj: JumioVerification): JsValue =
      SerializationUnsupported(obj)
  }

  implicit val jumioNetverifyInitParamsFormat = jsonFormat(JumioNetverifyInitParams,
    "merchantIdScanReference", "successUrl", "errorUrl", "callbackUrl", "customerId"
  )
  implicit val jumioNetverifyInitResponseFormat = jsonFormat(JumioNetverifyInitResponse,
    "authorizationToken", "jumioIdScanReference", "timestamp"
  )
  implicit val jumioMdNetverifyInitParamsFormat = jsonFormat(JumioMdNetverifyInitParams,
    "merchantScanReference", "successUrl", "errorUrl", "callbackUrl", "customerId", "country", "type"
  )
  implicit val jumioMdNetverifyInitResponseFormat = jsonFormat(JumioMdNetverifyInitResponse,
    "scanReference", "clientRedirectUrl"
  )

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

    override def write(obj: JumioAddress): JsValue =
      SerializationUnsupported(obj)
  }

  implicit val jumioExtractedDataReader = new RootJsonFormat[JumioExtractedData] {
    override def read(json: JsValue): JumioExtractedData = json match {
      case obj: JsObject =>
        val fields = obj.fields

        JumioExtractedData(
          fields.get("firstName").map(_.convertTo[String]),
          fields.get("lastName").map(_.convertTo[String]),
          fields.get("name").map(_.convertTo[String]),
          fields.get("ssn").map(_.convertTo[String]),
          fields.get("signatureAvailable").map(_.convertTo[Boolean]),
          fields.get("accountNumber").map(_.convertTo[String]),
          fields.get("issueDate").map(_.convertTo[String]),
          fields.get("address").map(com.snapswap.jumio.unmarshaller.jumioAddressReader.read)
        )
      case x => deserializationError("Expected address as object, but got " + x)
    }

    override def write(obj: JumioExtractedData): JsValue =
      SerializationUnsupported(obj)
  }

  {

    jsonFormat(JumioExtractedData.apply, "firstName", "lastName", "name", "ssn", "signatureAvailable", "accountNumber", "issueDate", "address")
  }

  implicit val jumioDocumentReader = jsonFormat(JumioDocument.apply, "type", "subType", "issuingCountry", "firstName", "lastName", "dob", "expiry", "number", "personalNumber", "address", "extractedData", "status")

  implicit val jumioScanReader = jsonFormat5(JumioScan)


  implicit val jumioImageFormat = new RootJsonFormat[JumioImage] {
    override def read(json: JsValue): JumioImage = {

      val f = json.asJsObject.fields

      val href =
        f.get("href")
          .map(_.convertTo[String])
          .getOrElse(throw JumioMalformedResponse(json.prettyPrint))

      val maskhint = f.get("maskhint").map(_.convertTo[EnumJumioImageMaskHint.ImageMaskHint])

      val classifier = f("classifier") match {
        case JsNumber(c) =>
          c.toInt.toString
        case JsString(s) =>
          s
        case _ =>
          throw JumioMalformedResponse(json.prettyPrint)
      }

      JumioImage(classifier = classifier, href = href, maskhint = maskhint)
    }

    override def write(obj: JumioImage): JsValue =
      SerializationUnsupported(obj)
  }

  implicit val jumioImagesInfoFormat = jsonFormat(JumioImagesInfo,
    "timestamp", "scanReference", "images"
  )
}

object unmarshaller extends JumioUnmarshaller
