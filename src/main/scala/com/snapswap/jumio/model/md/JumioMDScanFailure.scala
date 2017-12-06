package com.snapswap.jumio.model.md

import com.snapswap.jumio.model.{EnumJumioSources, JumioAddress, JumioFailureResult}


case class JumioMDScanFailure(merchantScanReference: String,
                              scanReference: String,
                              source: EnumJumioSources.JumioSource,
                              override val error: String,
                              override val details: String)
  extends JumioMDScanResult
    with JumioFailureResult {

  val firstName: Option[String] = None
  val lastName: Option[String] = None
  val name: Option[String] = None
  val ssn: Option[String] = None
  val signatureAvailable: Option[Boolean] = None
  val accountNumber: Option[String] = None
  val issueDateRawFormat: Option[String] = None
  val address: Option[JumioAddress] = None

  override def toString: String = {
    s"FAILURE multi document callback scanReference=$scanReference] with reason [$details]"
  }
}