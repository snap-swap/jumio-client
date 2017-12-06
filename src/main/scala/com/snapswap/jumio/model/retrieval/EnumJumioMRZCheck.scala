package com.snapswap.jumio.model.retrieval

object EnumJumioMRZCheck extends Enumeration {
  type JumioMRZCheck = Value
  val ok = Value("OK")
  val not_ok = Value("NOT_OK")
  val not_available = Value("NOT_AVAILABLE")
}
