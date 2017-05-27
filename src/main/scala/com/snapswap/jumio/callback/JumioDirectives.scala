package com.snapswap.jumio.callback

import akka.http.scaladsl.server.Directives.formFieldMap
import akka.http.scaladsl.server.Directive1

trait JumioDirectives {
  def jumioScanResult: Directive1[JumioScanResult] = formFieldMap.map(JumioScanResult.of)

  def jumioMDScanResult: Directive1[JumioMDScanResult] = formFieldMap.map(JumioMDScanResult.of)
}

object JumioDirectives extends JumioDirectives
