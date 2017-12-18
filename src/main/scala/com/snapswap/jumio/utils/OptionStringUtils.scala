package com.snapswap.jumio.utils

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


object OptionStringUtils {

  implicit class OptStringLifter(opt: Option[String]){
    def getOrUnknown: String =
      opt.getOrElse("UNKNOWN")

    def toDateTime: Option[ZonedDateTime] =
      opt.map(ZonedDateTime.parse(_, DateTimeFormatter.ISO_ZONED_DATE_TIME))
  }
}