package com.snapswap.jumio.model

trait JumioFailureResult extends JumioResult {
  def error: String

  def details: String
}