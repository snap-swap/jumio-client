package com.snapswap.jumio.model.errors


case object JumioEntityNotFoundError extends JumioError {
  override val details = "Jumio Netverify scan is not found"
}
