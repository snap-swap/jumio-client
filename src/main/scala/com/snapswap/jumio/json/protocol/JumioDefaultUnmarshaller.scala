package com.snapswap.jumio.json.protocol

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat, deserializationError}

import scala.util.{Failure, Success, Try}

trait JumioDefaultUnmarshaller extends {
  self: DefaultJsonProtocol =>

  implicit object DateJsonFormat extends RootJsonFormat[LocalDate] {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override def read(json: JsValue): LocalDate = json match {
      case JsString(str) =>
        Try(ZonedDateTime.parse(str, formatter)) match {
          case Success(dt) =>
            dt.toLocalDate
          case Failure(ex) =>
            deserializationError(s"Expected Date as JsString in '${formatter.toString}' format, but got '$str'", ex)
        }
      case other =>
        deserializationError(s"Expected DateTime as JsString, but got $other")
    }

    override def write(obj: LocalDate) =
      JsString(obj.format(formatter))
  }


  implicit object DateTimeJsonFormat extends RootJsonFormat[ZonedDateTime] {
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    override def read(json: JsValue): ZonedDateTime = json match {
      case JsString(str) =>
        Try(ZonedDateTime.parse(str, formatter)) match {
          case Success(dt) =>
            dt
          case Failure(ex) =>
            deserializationError(s"Expected Date as JsString in '${formatter.toString}' format, but got '$str'", ex)
        }
      case other =>
        deserializationError(s"Expected DateTime as JsString, but got $other")
    }

    override def write(obj: ZonedDateTime) =
      JsString(obj.format(formatter))
  }

}
