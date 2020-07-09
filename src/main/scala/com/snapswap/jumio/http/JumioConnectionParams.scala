package com.snapswap.jumio.http


sealed trait JumioConnectionParams {
  def apiHost: String

  def token: String

  def secret: String
}

case class JumioNetverifyConnectionParams(apiHost: String, token: String, secret: String) extends JumioConnectionParams

case class JumioRetrievalConnectionParams(apiHost: String, token: String, secret: String) extends JumioConnectionParams