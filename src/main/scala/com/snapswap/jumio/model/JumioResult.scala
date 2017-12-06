package com.snapswap.jumio.model


trait JumioResult {
  def merchantScanReference: String

  def scanReference: String

  def source: EnumJumioSources.JumioSource
}

