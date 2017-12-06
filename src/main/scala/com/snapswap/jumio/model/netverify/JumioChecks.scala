package com.snapswap.jumio.model.netverify


case class JumioChecks(dataPositions: Boolean,
                       documentValidation: Boolean,
                       hologram: Boolean,
                       mrzCode: Boolean,
                       microprint: Boolean,
                       securityFeatures: Boolean,
                       signature: Boolean) {
  private def okOrNA(check: Boolean) = if (check) "OK" else "N/A"

  override def toString: String = (
    "dataPositions=" + okOrNA(dataPositions) ::
      "documentValidation=" + okOrNA(documentValidation) ::
      "hologram=" + okOrNA(hologram) ::
      "mrzCode=" + okOrNA(mrzCode) ::
      "microprint=" + okOrNA(microprint) ::
      "securityFeatures=" + okOrNA(securityFeatures) ::
      "signature=" + okOrNA(signature) ::
      Nil).mkString("checked [", ", ", "]")
}

object JumioChecks {

  def of(parameters: Map[String, String]): JumioChecks = {

    def isOk(name: String) = parameters.getOrElse(name, "") == "OK"

    JumioChecks(
      dataPositions = isOk("idCheckDataPositions"),
      documentValidation = isOk("idCheckDocumentValidation"),
      hologram = isOk("idCheckHologram"),
      mrzCode = isOk("idCheckMRZcode"),
      microprint = isOk("idCheckMicroprint"),
      securityFeatures = isOk("idCheckSecurityFeatures"),
      signature = isOk("idCheckSignature")
    )
  }
}
