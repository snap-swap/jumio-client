package com.snapswap.jumio

trait JumioError extends RuntimeException {
  def details: String
  def cause: Option[Throwable] = None
  override def getMessage = details
  override def getCause = cause.orNull
}

case object JumioEntityNotFoundError extends JumioError {
  override val details = "Jumio Netverify scan is not found"
}

case class JumioConnectionError(details: String, override val cause: Option[Throwable] = None) extends JumioError

case class JumioMalformedResponse(responseEntity: String, override val cause: Option[Throwable] = None) extends JumioError {
  override val details = s"Jumio response is malformed: $responseEntity"
}
