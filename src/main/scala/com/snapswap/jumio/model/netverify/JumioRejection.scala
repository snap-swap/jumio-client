package com.snapswap.jumio.model.netverify

case class JumioRejection(code: String,
                          description: String) {
  override def toString: String = s"[$code] $description"
}
