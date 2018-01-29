package com.snapswap.jumio

import com.snapswap.jumio.model.EnumJumioDocTypes
import com.snapswap.jumio.model.init.{JumioMdNetverifyInitResponse, JumioNetverifyInitResponse}
import com.snapswap.jumio.model.netverify.PerformNetverifyResponse
import com.snapswap.jumio.model.retrieval.JumioImageRawData

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

  def performNetverify(merchantIdScanReference: String,
                       country: String,
                       face: JumioImageRawData,
                       idType: EnumJumioDocTypes.JumioDocType,
                       idFront: JumioImageRawData,
                       idBack: Option[JumioImageRawData],
                       callbackUrl: String): Future[PerformNetverifyResponse]
}