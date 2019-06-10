package com.snapswap.jumio

import com.snapswap.jumio.model.EnumJumioDocTypes
import com.snapswap.jumio.model.init.{JumioMdNetverifyInitResponse, JumioNetverifyInitResponseV3, JumioNetverifyInitResponseV4}
import com.snapswap.jumio.model.netverify.{AcceptedIdDocs, PerformNetverifyResponse}
import com.snapswap.jumio.model.retrieval.JumioImageRawData

import scala.concurrent.Future


trait NetverifyClient {

  def initNetverifyV3(merchantScanReference: String,
                      redirectUrl: String,
                      callbackUrl: String,
                      customerId: String): Future[JumioNetverifyInitResponseV3]

  def initNetverifyV4(merchantScanReference: String,
                      redirectUrl: String,
                      callbackUrl: String,
                      customerId: String): Future[JumioNetverifyInitResponseV4]

  def initMdNetverify(merchantScanReference: String,
                      redirectUrl: String,
                      callbackUrl: String,
                      customerId: String,
                      country: String,
                      docType: EnumJumioDocTypes.JumioDocType): Future[JumioMdNetverifyInitResponse]

  def performNetverify(merchantIdScanReference: String,
                       country: String,
                       face: Option[JumioImageRawData],
                       idType: EnumJumioDocTypes.JumioDocType,
                       idFront: JumioImageRawData,
                       idBack: Option[JumioImageRawData],
                       callbackUrl: String,
                       customerId: Option[String],
                       clientIp: Option[String]): Future[PerformNetverifyResponse]

  def listAcceptedIdDocs(): Future[AcceptedIdDocs]
}