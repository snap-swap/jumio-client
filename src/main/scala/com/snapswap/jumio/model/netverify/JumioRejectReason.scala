package com.snapswap.jumio.model.netverify

case class JumioRejectReason(code: String,
                             description: String,
                             details: Seq[JumioRejection]) {
  override def toString: String = {
    s"[$code] $description" + (
      if (details.isEmpty)
        ""
      else
        " " + details.map(s => s"[${s.code}] ${s.description}").mkString("(", ", ", ")")
      )
  }
}
