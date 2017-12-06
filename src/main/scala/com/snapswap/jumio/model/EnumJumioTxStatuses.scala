package com.snapswap.jumio.model


object EnumJumioTxStatuses extends Enumeration {
  type JumioTxStatus = Value
  val pending = Value("PENDING")
  val done = Value("DONE")
  val failed = Value("FAILED")
}