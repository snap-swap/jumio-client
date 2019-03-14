package com.snapswap.jumio.model.errors

case class CallbackParsingError(details: String, override val cause: Option[Throwable]) extends JumioError

object CallbackParsingError {
  def apply(details: String, cause: Throwable): CallbackParsingError =
    new CallbackParsingError(details, Some(cause))

  def apply(details: String): CallbackParsingError =
    new CallbackParsingError(details, None)
}
