package com.snapswap.jumio.callback

import com.snapswap.jumio.EnumJumioSources


trait JumioNetverifyResult {
  def merchantScanReference: String

  def scanReference: String

  def source: EnumJumioSources.JumioSource
}

trait JumioNetverifyFailure extends JumioNetverifyResult {
  def error: String

  def details: String
}