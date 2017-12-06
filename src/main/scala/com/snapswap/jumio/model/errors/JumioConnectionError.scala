package com.snapswap.jumio.model.errors


case class JumioConnectionError(details: String,
                                override val cause: Option[Throwable]) extends JumioError

object JumioConnectionError {
  def apply(details: String): JumioConnectionError =
    JumioConnectionError(details, None)

  def apply(details: String, cause: Throwable): JumioConnectionError =
    JumioConnectionError(details, Some(cause))
}
