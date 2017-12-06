package com.snapswap.jumio.model


object EnumJumioDocTypes extends Enumeration {
  type JumioDocType = Value
  val passport = Value("PASSPORT")
  val driving_license = Value("DRIVING_LICENSE")
  val id_card = Value("ID_CARD")
  val unsupported = Value("UNSUPPORTED")

  // Multi-Document types
  val bs = Value("BS") // Bank statement, front side
  val ic = Value("IC") // Insurance card, front side
  val ub = Value("UB") //  Utility bill, front side
  val caap = Value("CAAP") //  Cash advance application, front and back side
  val crc = Value("CRC") //  Corporate resolution certificate, front and back side
  val ccs = Value("CCS") //  Credit card statement, front and back side
  val lag = Value("LAG") //  Lease agreement, front and back side
  val loap = Value("LOAP") //  Loan application, front and back side
  val moap = Value("MOAP") //  Mortgage application, front and back side
  val tr = Value("TR") //  Tax return, front and back side
  val vt = Value("VT") //  Vehicle title, front side
  val vc = Value("VC") //  Voided check, front side
  val stuc = Value("STUC") //  Student card, front side
  val hcc = Value("HCC") //  Health care card, front side
  val cb = Value("CB") //  Council bill, front side
  val senc = Value("SENC") //  Seniors card, front side
  val medc = Value("MEDC") //  Medicare card, front side
  val bc = Value("BC") //  Birth certificate, front side
  val wwcc = Value("WWCC") //  Working with children check, front side
  val ss = Value("SS") //  Superannuation statement, front side
  val tac = Value("TAC") //  Trade association card, front side
  val sel = Value("SEL") //  School enrolment letter, front side
  val pb = Value("PB") //  Phone bill, front side
  val usss = Value("USSS") //  US social security card, front side
  val ssc = Value("SSC") //  Social security card, front side
  val custom = Value("CUSTOM") //  Custom document type
}
