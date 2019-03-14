package com.snapswap.jumio.model

import com.snapswap.jumio.model.errors.CallbackParsingError

import scala.util.{Failure, Success, Try}


case class JumioRawResult(merchantScanReference: String,
                          scanReference: String,
                          source: EnumJumioSources.JumioSource,
                          rawData: Map[String, String]) extends JumioResult

object JumioRawResult {

  def of(parameters: Map[String, String]): JumioRawResult = Try(JumioRawResult(
    merchantScanReference = parameters("merchantIdScanReference"),
    scanReference = parameters("jumioIdScanReference"),
    source = parameters.get("idScanSource").map(EnumJumioSources.withName)
      .getOrElse(EnumJumioSources.unknown),
    rawData = parameters
  )) match {
    case Success(value) =>
      value
    case Failure(exception) =>
      throw CallbackParsingError("can't extract base info", exception)
  }
}
