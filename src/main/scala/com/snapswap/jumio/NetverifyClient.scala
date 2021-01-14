package com.snapswap.jumio

import com.snapswap.jumio.http.JumioNetverifyConnectionParams
import com.snapswap.jumio.model.EnumJumioDocTypes
import com.snapswap.jumio.model.init.{JumioNetverifyInitResponseV3, JumioNetverifyInitResponseV4}
import com.snapswap.jumio.model.netverify.{AcceptedIdDocs, PerformNetverifyResponse}
import com.snapswap.jumio.model.retrieval.JumioImageRawData

import scala.concurrent.Future


trait NetverifyClient {

  //FIXME check usages for front-end, remove as unsupported by jumio
  def initNetverifyV3(merchantScanReference: String,
                      redirectUrl: Option[String],
                      callbackUrl: String,
                      customerId: String)
                     (implicit params: JumioNetverifyConnectionParams): Future[JumioNetverifyInitResponseV3]

  def initNetverifyV4(merchantScanReference: String,
                      redirectUrl: Option[String],
                      callbackUrl: String,
                      customerId: String,
                      locale: Option[String],
                      workflowId: Option[Int])
                     (implicit params: JumioNetverifyConnectionParams): Future[JumioNetverifyInitResponseV4]

  def performNetverify(merchantIdScanReference: String,
                       country: String,
                       face: Option[JumioImageRawData],
                       idType: EnumJumioDocTypes.JumioDocType,
                       idFront: JumioImageRawData,
                       idBack: Option[JumioImageRawData],
                       callbackUrl: String,
                       customerId: Option[String],
                       clientIp: Option[String])
                      (implicit params: JumioNetverifyConnectionParams): Future[PerformNetverifyResponse]

  def listAcceptedIdDocs()(implicit params: JumioNetverifyConnectionParams): Future[AcceptedIdDocs]
}