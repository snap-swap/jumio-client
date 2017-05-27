package com.snapswap.jumio.callback

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object Extractors {
  implicit class MapImplicits(parameters: Map[String, String]) {
    def getString(name: String): Option[String] = parameters.get(name)

    def getOrUnknown(name: String): String = getString(name).getOrElse("UNKNOWN")

    def getTimestamp(name: String): Option[DateTime] = parameters.get(name).map(ISODateTimeFormat.dateTime().withZoneUTC().parseDateTime)
  }
}