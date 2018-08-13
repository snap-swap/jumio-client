package com.snapswap.jumio.model

case class JumioMrzData(data: Seq[String]) {
  override def toString: String = {
    "MRZ data is '" + data.mkString(", ") + "'"
  }
}