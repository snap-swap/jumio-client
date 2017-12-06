package com.snapswap.jumio.model.init

import com.snapswap.jumio.model.EnumJumioDocTypes


case class JumioMdNetverifyInitParams(merchantScanReference: String,
                                      successUrl: String,
                                      errorUrl: String,
                                      callbackUrl: String,
                                      customerId: String,
                                      country: String,
                                      docType: EnumJumioDocTypes.JumioDocType) {
  require(country == country.toUpperCase && country.length == 3, "Country should be an upper-cased 3-letters string")
}
