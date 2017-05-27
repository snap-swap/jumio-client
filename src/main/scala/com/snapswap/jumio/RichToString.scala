package com.snapswap.jumio

private[jumio] object RichToString {

  implicit class ToString[T](value: Option[T]) {
    def orUnknown: String = {
      value.map(_.toString).getOrElse("N/A")
    }
  }

}