package com.snapswap.jumio.json.protocol

import com.snapswap.jumio.model
import com.snapswap.jumio.model.EnumJumioDocTypes.JumioDocType
import com.snapswap.jumio.model._
import com.snapswap.jumio.model.errors.JumioMalformedResponse
import com.snapswap.jumio.model.init._
import com.snapswap.jumio.model.netverify._
import com.snapswap.jumio.model.retrieval._
import spray.json._

import scala.util.{Failure, Success, Try}


trait JumioUnmarshaller
  extends DefaultJsonProtocol
    with JumioDefaultUnmarshaller
    with JumioEnumsUnmarshaller
    with JumioAddressUnmarshaller {

  protected def enumNameFormat(enum: Enumeration): RootJsonFormat[enum.Value] = new RootJsonFormat[enum.Value] {
    def write(v: enum.Value): JsValue =
      JsString(v.toString.replace("$minus", "-"))

    def read(value: JsValue): enum.Value = value match {
      case JsString(s) => enum.withName(s)
      case JsNumber(n) => enum.withName(n.toString())
      case x => deserializationError(s"Expected $enum as JsString, but got $x")
    }
  }

  private implicit class JsonLifter(json: JsValue) {
    def convertWithNullCheck[T](implicit reader: JsonReader[T]): Option[T] = json match {
      case JsNull =>
        None
      case j =>
        Some(j.convertTo[T])
    }
  }

  implicit val jumioScanStatusFormat = jsonFormat3(JumioScanStatus)
  implicit val jumioTxFormat = jsonFormat(JumioTx,
    "status", "source", "date", "clientIp", "customerId", "additionalInformation",
    "merchantScanReference", "merchantReportingCriteria"
  )
  implicit val jumioRejectionFormat = jsonFormat(JumioRejection, "detailsCode", "detailsDescription")

  implicit object JumioRejectReasonReader extends JumioJsonReader[JumioRejectReason] {
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
  }


  implicit object identityVerificationReader extends JumioJsonReader[IdentityVerification] {
    override def read(json: JsValue): IdentityVerification = json match {
      case obj: JsObject =>
        val fields = obj.fields

        model.IdentityVerification(
          fields.get("similarity").map(_.convertTo[EnumJumioSimilarity.JumioSimilarity]).get,
          fields.get("validity").map {
            case JsString(value) =>
              value.toBoolean
            case JsBoolean(value) =>
              value
            case other =>
              deserializationError(s"Expected Boolean , but got $other")
          }.get,
          fields.get("reason").map(_.convertTo[EnumJumioIdVerificationFailureReasons.JumioIdVerificationFailureReasons])
        )
      case x => deserializationError("Expected JumioVerification as object, but got " + x)
    }
  }

  implicit object JumioVerificationReader extends JumioJsonReader[JumioVerification] {
    override def read(json: JsValue): JumioVerification = json match {
      case obj: JsObject =>
        val fields = obj.fields

        JumioVerification(
          fields.get("mrzCheck").map(_.convertTo[EnumJumioMRZCheck.JumioMRZCheck]),
          fields.get("identityVerification").flatMap(_.convertWithNullCheck[IdentityVerification]),
          fields.get("faceMatch").map(_.convertTo[String]).map(_.toFloat)
        )
      case x => deserializationError("Expected JumioVerification as object, but got " + x)
    }
  }

  implicit val jumioNetverifyInitParamsV3Format = jsonFormat(JumioNetverifyInitParamsV3,
    "merchantIdScanReference", "successUrl", "errorUrl", "callbackUrl", "customerId"
  )
  implicit val jumioNetverifyInitParamsV4Format = jsonFormat(JumioNetverifyInitParamsV4,
    "customerInternalReference", "successUrl", "errorUrl", "callbackUrl", "userReference"
  )
  implicit val jumioNetverifyInitResponseV3Format = jsonFormat(JumioNetverifyInitResponseV3,
    "authorizationToken", "jumioIdScanReference", "timestamp"
  )
  implicit val jumioNetverifyInitResponseV4Format = jsonFormat(JumioNetverifyInitResponseV4,
    "transactionReference", "redirectUrl", "timestamp"
  )

  implicit object jumioExtractedDataReader extends JumioJsonReader[JumioExtractedData] {

    import com.snapswap.jumio.utils.OptionStringUtils._

    override def read(json: JsValue): JumioExtractedData = json match {
      case obj: JsObject =>
        val fields = obj.fields

        model.JumioExtractedData(
          fields.get("firstName").map(_.convertTo[String]),
          fields.get("lastName").map(_.convertTo[String]),
          fields.get("name").map(_.convertTo[String]),
          fields.get("ssn").map(_.convertTo[String]),
          fields.get("signatureAvailable").map(_.convertTo[Boolean]),
          fields.get("accountNumber").map(_.convertTo[String]),
          fields.get("issueDate").map(_.convertTo[String]).toDateTime,
          fields.get("address").map(_.convertTo[JumioAddress])
        )
      case x => deserializationError("Expected address as object, but got " + x)
    }
  }

  implicit val JumioGenderEnumReader = enumNameFormat(JumioGenderEnum)

  implicit val MrzDataReader = jsonFormat(JumioMrzData, "data")

  implicit val jumioDocumentReader = jsonFormat(JumioDocument.apply,
    "type",
    "subType",
    "issuingCountry",
    "issueDate",
    "firstName",
    "lastName",
    "dob",
    "expiry",
    "number",
    "gender",
    "nationality",
    "personalNumber",
    "address",
    "extractedData",
    "status",
    "mrz",
    "placeOfBirth"
  )
  implicit val jumioScanReader = jsonFormat5(JumioScan)


  implicit object JumioImageReader extends JumioJsonReader[JumioImage] {
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
  }

  implicit val jumioImagesReader = new RootJsonFormat[JumioImagesInfo] {
    override def read(json: JsValue): JumioImagesInfo = {
      json match {
        case obj: JsObject =>
          (obj.fields.get("timestamp"), obj.fields.get("scanReference"), obj.fields.get("images")) match {
            case (Some(timestamp), Some(scanReference), Some(images)) =>
              val imagesSeq: Seq[JumioImage] = images match {
                case arr: JsArray => arr.convertTo[Seq[JumioImage]]
                case img: JsObject => Seq(img.convertTo[JumioImage])
                case x => deserializationError(s"Expected 'images' as either JsArray or JsObject but found $obj")
              }
              JumioImagesInfo(timestamp.convertTo[String], scanReference.convertTo[String], imagesSeq)
            case _ => deserializationError(s"Expected JumioImagesInfo as JsObject with 'timestamp', 'scanReference', and 'images' but found $obj")
          }
        case x => deserializationError(s"Expected JumioImagesInfo as JsObject but found $x")
      }
    }

    override def write(obj: JumioImagesInfo): JsValue = JsObject(Map(
      "timestamp" -> JsString(obj.timestamp),
      "scanReference" -> JsString(obj.scanReference),
      "images" -> obj.images.toJson
    ))
  }

  implicit val performNetverifyRequestFormat = jsonFormat(PerformNetverifyRequest,
    "merchantIdScanReference",
    "faceImage",
    "faceImageMimeType",
    "frontsideImage",
    "frontsideImageMimeType",
    "backsideImage",
    "backsideImageMimeType",
    "country",
    "idType",
    "callbackUrl",
    "enabledFields",
    "customerId",
    "clientIp"
  )

  implicit val performNetverifyResponseFormat = jsonFormat(PerformNetverifyResponse,
    "jumioIdScanReference",
    "timestamp"
  )

  implicit object AcceptedIdDocsReader extends RootJsonReader[AcceptedIdDocs] {
    override def read(json: JsValue): AcceptedIdDocs = Try {
      json.asJsObject.fields("acceptedIdTypes").convertTo[Seq[JsObject]].map { i =>
        i.fields("countryCode").convertTo[String] -> i.fields("idTypes").convertTo[Seq[JsObject]].map { d =>
          d.fields("idType").convertTo[JumioDocType] ->
            d.fields("acquisitionConfig").asJsObject.fields("backSide").convertTo[Boolean]
        }
      }
    } match {
      case Success(a) =>
        a.toMap
      case Failure(ex) =>
        deserializationError(s"can't unmarshall into AcceptedIdDocs, json:\n${json.prettyPrint}\n", ex)
    }
  }

}

object JumioUnmarshaller extends JumioUnmarshaller
