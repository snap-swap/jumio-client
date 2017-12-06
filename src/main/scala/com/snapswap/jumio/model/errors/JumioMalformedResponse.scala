package com.snapswap.jumio.model.errors



case class JumioMalformedResponse(responseEntity: String,
                                  override val cause: Option[Throwable]) extends JumioError {
  override val details = s"Jumio response is malformed: $responseEntity"
}

object JumioMalformedResponse {
  def apply(responseEntity: String): JumioMalformedResponse =
    JumioMalformedResponse(responseEntity, None)

  def apply(responseEntity: String, cause: Throwable): JumioMalformedResponse =
    JumioMalformedResponse(responseEntity, Some(cause))
}
