package com.snapswap.jumio.model

import java.time.ZonedDateTime


case class JumioExtractedData(firstName: Option[String],
                              lastName: Option[String],
                              name: Option[String],
                              ssn: Option[String],
                              signatureAvailable: Option[Boolean],
                              accountNumber: Option[String],
                              issueDate: Option[ZonedDateTime],
                              address: Option[JumioAddress])