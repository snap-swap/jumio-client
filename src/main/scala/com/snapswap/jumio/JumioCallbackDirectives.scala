package com.snapswap.jumio

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.formFieldMap
import com.snapswap.jumio.model.JumioRawResult
import com.snapswap.jumio.model.md.JumioMDScanResult


trait JumioCallbackDirectives {
  def jumioScanResult: Directive1[JumioRawResult] = formFieldMap.map(JumioRawResult.of)

  def jumioMDScanResult: Directive1[JumioMDScanResult] = formFieldMap.map(JumioMDScanResult.of)
}

object JumioCallbackDirectives extends JumioCallbackDirectives
