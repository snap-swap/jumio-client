package com.snapswap.jumio.model


case class JumioAddress(country: String,
                        region: Option[String],
                        city: String,
                        postalCode: String,
                        streetAddress: String,
                        protected val format: EnumJumioAddressFormats.JumioAddressFormat) {
  override def toString = s"$streetAddress, $postalCode $city, $country"
}