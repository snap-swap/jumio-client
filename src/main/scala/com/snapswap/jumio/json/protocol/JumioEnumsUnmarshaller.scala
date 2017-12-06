package com.snapswap.jumio.json.protocol

import com.snapswap.jumio.model._
import com.snapswap.jumio.model.retrieval.{EnumJumioImageMaskHint, EnumJumioMRZCheck}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat, deserializationError}

trait JumioEnumsUnmarshaller {
  self: DefaultJsonProtocol =>

  private def enumNameFormat(enum: Enumeration) = new RootJsonFormat[enum.Value] {
    def read(value: JsValue): enum.Value = value match {
      case JsString(s) => enum.withName(s)
      case x => deserializationError("Expected Enum as JsString, but got " + x)
    }

    def write(v: enum.Value): JsValue = JsString(v.toString)
  }


  implicit val enumJumioTxStatusFormat = enumNameFormat(EnumJumioTxStatuses)
  implicit val enumJumioSourceFormat = enumNameFormat(EnumJumioSources)
  implicit val enumMRZCheckFormat = enumNameFormat(EnumJumioMRZCheck)
  implicit val enumJumioDocTypeFormat = enumNameFormat(EnumJumioDocTypes)
  implicit val enumJumioDocumentStatusFormat = enumNameFormat(EnumJumioDocumentStatus)
  implicit val enumJumioImageMaskHintFormat = enumNameFormat(EnumJumioImageMaskHint)
  implicit val enumJumioSimilarityFormat = enumNameFormat(EnumJumioSimilarity)
  implicit val enumJumioIdVerificationFailureReasonsFormat = enumNameFormat(EnumJumioIdVerificationFailureReasons)
}
