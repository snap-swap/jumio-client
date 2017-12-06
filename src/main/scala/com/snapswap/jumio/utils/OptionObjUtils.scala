package com.snapswap.jumio.utils

object OptionObjUtils {

  implicit class OptLifter[T](value: Option[T]) {
    def toStringOrNA: String = {
      value.map(_.toString).getOrElse("N/A")
    }
  }
}