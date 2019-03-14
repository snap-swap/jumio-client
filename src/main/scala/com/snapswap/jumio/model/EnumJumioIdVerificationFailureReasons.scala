package com.snapswap.jumio.model

object EnumJumioIdVerificationFailureReasons extends Enumeration {
  type JumioIdVerificationFailureReasons = Value

  val SELFIE_CROPPED_FROM_ID = Value("SELFIE_CROPPED_FROM_ID")
  val ENTIRE_ID_USED_AS_SELFIE = Value("ENTIRE_ID_USED_AS_SELFIE")
  val MULTIPLE_PEOPLE = Value("MULTIPLE_PEOPLE")
  val SELFIE_IS_SCREEN_PAPER_VIDEO = Value("SELFIE_IS_SCREEN_PAPER_VIDEO")
  val SELFIE_MANIPULATED = Value("SELFIE_MANIPULATED")
  val AGE_DIFFERENCE_TOO_BIG = Value("AGE_DIFFERENCE_TOO_BIG")
  val NO_FACE_PRESENT = Value("NO_FACE_PRESENT")
  val FACE_NOT_FULLY_VISIBLE = Value("FACE_NOT_FULLY_VISIBLE")
  val BAD_QUALITY = Value("BAD_QUALITY")
  val LIVENESS_FAILED = Value("LIVENESS_FAILED")
}





