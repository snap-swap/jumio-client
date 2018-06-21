package com.snapswap.jumio.utils

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.time.LocalDate

object OptionStringUtils {

  implicit class OptStringLifter(opt: Option[String]){
    def getOrUnknown: String =
      opt.getOrElse("UNKNOWN")

    def toDateTime: Option[ZonedDateTime] =
      opt.map(ZonedDateTime.parse(_, DateTimeFormatter.ISO_ZONED_DATE_TIME))

    def toDate: Option[ZonedDateTime] =
      opt.map { str =>
        val date = LocalDate.parse(str, DateTimeFormatter.ISO_DATE)
        date.atStartOfDay(ZoneId.of("UTC"))
      }
  }
}