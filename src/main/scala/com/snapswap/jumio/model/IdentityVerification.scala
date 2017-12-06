package com.snapswap.jumio.model

import com.snapswap.jumio.json.protocol.JumioUnmarshaller._
import spray.json._


object IdentityVerification {
  def of(parameters: Map[String, String]): Option[IdentityVerification] = {
    parameters.get("identityVerification").map(_.parseJson.convertTo[IdentityVerification])
  }
}

case class IdentityVerification(similarity: EnumJumioSimilarity.JumioSimilarity,
                                validity: Boolean,
                                reason: Option[EnumJumioIdVerificationFailureReasons.JumioIdVerificationFailureReasons]) {
  override def toString = {
    s"similarity is $similarity, look like " +
      s"${if (validity) "valid" else "invalid"}" +
      s"${reason.map(r => s" because $r").getOrElse("")}"
  }
}