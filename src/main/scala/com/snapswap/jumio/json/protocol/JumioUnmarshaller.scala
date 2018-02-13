package com.snapswap.jumio.json.protocol

import com.snapswap.jumio.model
import com.snapswap.jumio.model._
import com.snapswap.jumio.model.errors.JumioMalformedResponse
import com.snapswap.jumio.model.init.{JumioMdNetverifyInitParams, JumioMdNetverifyInitResponse, JumioNetverifyInitParams, JumioNetverifyInitResponse}
import com.snapswap.jumio.model.netverify.{JumioRejectReason, JumioRejection, PerformNetverifyRequest, PerformNetverifyResponse}
import com.snapswap.jumio.model.retrieval._
import spray.json._


trait JumioUnmarshaller
  extends DefaultJsonProtocol
    with JumioDefaultUnmarshaller
    with JumioEnumsUnmarshaller
    with JumioAddressUnmarshaller {

  private implicit class JsonLifter(json: JsValue){
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

  implicit object jumioExtractedDataReader extends JumioJsonReader[JumioExtractedData] {
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
          fields.get("issueDate").map(_.convertTo[String]),
          fields.get("address").map(_.convertTo[JumioAddress])
        )
      case x => deserializationError("Expected address as object, but got " + x)
    }
  }

  implicit val jumioDocumentReader = jsonFormat(JumioDocument.apply,
    "type",
    "subType",
    "issuingCountry",
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
    "status"
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

  implicit val jumioImagesInfoFormat = jsonFormat(JumioImagesInfo,
    "timestamp",
    "scanReference",
    "images"
  )

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
    "enabledFields"
  )

  implicit val performNetverifyResponseFormat = jsonFormat(PerformNetverifyResponse,
    "jumioIdScanReference",
    "timestamp"
  )
}

object JumioUnmarshaller extends JumioUnmarshaller
