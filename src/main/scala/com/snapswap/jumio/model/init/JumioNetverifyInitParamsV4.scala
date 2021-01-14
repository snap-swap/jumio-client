package com.snapswap.jumio.model.init

case class JumioNetverifyInitParamsV4(customerInternalReference: String, //the same as merchantIdScanReference in v3
                                      successUrl: Option[String],
                                      errorUrl: Option[String],
                                      callbackUrl: String,
                                      userReference: String, //the same as customerId in v3
                                      locale: Option[String],
                                      workflowId: Option[Int])
