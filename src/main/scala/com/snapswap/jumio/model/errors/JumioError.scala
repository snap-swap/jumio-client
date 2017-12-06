package com.snapswap.jumio.model.errors

trait JumioError extends RuntimeException {
  def details: String

  def cause: Option[Throwable] = None

  override def getMessage = details

  override def getCause = cause.orNull
}

