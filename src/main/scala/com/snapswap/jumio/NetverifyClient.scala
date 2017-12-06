package com.snapswap.jumio

import com.snapswap.jumio.model.EnumJumioDocTypes
import com.snapswap.jumio.model.init.{JumioMdNetverifyInitResponse, JumioNetverifyInitResponse}

import scala.concurrent.Future


trait NetverifyClient {

  def initNetverify(merchantScanReference: String,
                    redirectUrl: String,
                    callbackUrl: String,
                    customerId: String): Future[JumioNetverifyInitResponse]

  def initMdNetverify(merchantScanReference: String,
                      redirectUrl: String,
                      callbackUrl: String,
                      customerId: String,
                      country: String,
                      docType: EnumJumioDocTypes.JumioDocType): Future[JumioMdNetverifyInitResponse]
}


