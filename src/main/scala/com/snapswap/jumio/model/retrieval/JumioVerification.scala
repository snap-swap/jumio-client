package com.snapswap.jumio.model.retrieval

import com.snapswap.jumio.model.IdentityVerification


case class JumioVerification(mrzCheck: Option[EnumJumioMRZCheck.JumioMRZCheck],
                             identityVerification: Option[IdentityVerification],
                             faceMatch: Option[Float]) {
  def rejected: Boolean = identityVerification.flatMap(_.reason).isDefined
}