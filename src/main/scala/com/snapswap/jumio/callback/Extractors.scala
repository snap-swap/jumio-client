package com.snapswap.jumio.callback

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object Extractors {

  implicit class MapImplicits(parameters: Map[String, String]) {
    def getString(name: String): Option[String] = parameters.get(name)

    def getOrUnknown(name: String): String = getString(name).getOrElse("UNKNOWN")

    def getTimestamp(name: String): Option[ZonedDateTime] =
      parameters.get(name).map(ZonedDateTime.parse(_, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }

}