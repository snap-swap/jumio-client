package com.snapswap.jumio.model.netverify

case class PerformNetverifyRequest(merchantIdScanReference: String,
                                   faceImage: Option[String],
                                   faceImageMimeType: Option[String],
                                   frontsideImage: String,
                                   frontsideImageMimeType: String,
                                   backsideImage: Option[String],
                                   backsideImageMimeType: Option[String],
                                   country: String,
                                   idType: String,
                                   callbackUrl: String,
                                   enabledFields: String = "idNumber,idFirstName,idLastName,idDob,idExpiry,idUsState,idPersonalNumber,idFaceMatch,idAddress"
                                  )