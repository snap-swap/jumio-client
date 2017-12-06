package com.snapswap.jumio

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.formFieldMap
import com.snapswap.jumio.model.md.JumioMDScanResult
import com.snapswap.jumio.model.netverify.JumioScanResult


trait JumioCallbackDirectives {
  def jumioScanResult: Directive1[JumioScanResult] = formFieldMap.map(JumioScanResult.of)

  def jumioMDScanResult: Directive1[JumioMDScanResult] = formFieldMap.map(JumioMDScanResult.of)
}

object JumioCallbackDirectives extends JumioCallbackDirectives
