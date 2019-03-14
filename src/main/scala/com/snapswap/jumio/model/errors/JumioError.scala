package com.snapswap.jumio.model.errors

trait JumioError extends RuntimeException {
  def details: String

  def cause: Option[Throwable] = None

  override def getMessage: String = details

  override def getCause: Throwable = cause.orNull
}

