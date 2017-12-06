package com.snapswap.jumio.model

object EnumJumioSimilarity extends Enumeration {
  type JumioSimilarity = Value

  val MATCH = Value("MATCH")
  val NO_MATCH = Value("NO_MATCH")
  val NOT_POSSIBLE = Value("NOT_POSSIBLE")
}