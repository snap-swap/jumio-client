package com.snapswap.jumio.model


case class JumioExtractedData(firstName: Option[String],
                              lastName: Option[String],
                              name: Option[String],
                              ssn: Option[String],
                              signatureAvailable: Option[Boolean],
                              accountNumber: Option[String],
                              issueDate: Option[String],
                              address: Option[JumioAddress])