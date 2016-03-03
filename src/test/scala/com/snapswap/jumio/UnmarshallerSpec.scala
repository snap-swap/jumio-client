package com.snapswap.jumio

import org.joda.time.{DateTime, DateTimeZone}
import spray.json._
import org.scalatest._

class UnmarshallerSpec extends FlatSpec with Matchers {

  import unmarshaller._

  "Unmarshaller" should "parse details of PENDING scan " in {
    val result = pendingScan.parseJson.convertTo[JumioScan]
    result.transaction.status shouldBe EnumJumioTxStatuses.pending
    result.transaction.source shouldBe EnumJumioSources.sdk
    result.transaction.date shouldBe DateTime.now(DateTimeZone.UTC).withDate(2016, 1, 1).withTime(0, 0, 0, 0)
  }

  val pendingScan =
    """{
      |	"timestamp": "2014-08-14T08:16:20.845Z",
      |	"scanReference": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      |	"document": {
      |	},
      |	"transaction": {
      |		"clientIp": "xxx.xxx.xxx.xxx",
      |		"customerId": "CUSTOMERID",
      |		"date": "2016-01-01T00:00:00.000Z",
      |		"source": "SDK",
      |		"status": "PENDING"
      |	}
      |}""".stripMargin

  val failedScan =
    """{
      |	"timestamp": "2014-08-14T08:16:20.845Z",
      |	"scanReference": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      |	"document": {
      |	},
      |	"transaction": {
      |		"clientIp": "xxx.xxx.xxx.xxx",
      |		"customerId": "CUSTOMERID",
      |		"date": "2016-01-01T00:00:00.000Z",
      |		"source": "SDK",
      |		"status": "FAILED"
      |	},
      |	"verification": {
      |		"rejectReason": {
      |			"rejectReasonCode": "100",
      |			"rejectReasonDescription": "MANIPULATED_DOCUMENT"
      |		}
      |	}
      |}""".stripMargin

  val doneScan =
    """{
      |	"timestamp": "2014-08-14T08:16:20.845Z",
      |	"scanReference": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      |	"document": {
      |		"type": "PASSPORT",
      |		"dob": "1990-01-01",
      |		"expiry": "2022-12-31",
      |		"firstName": "FIRSTNAME",
      |		"issuingCountry": "USA",
      |		"lastName": "LASTNAME",
      |		"number": "P1234",
      |		"status": "APPROVED_VERIFIED"
      |	},
      |	"transaction": {
      |		"clientIp": "xxx.xxx.xxx.xxx",
      |		"customerId": "CUSTOMERID",
      |		"date": "2016-01-01T00:00:00.000Z",
      |		"source": "SDK",
      |		"status": "DONE"
      |	},
      |	"verification": {
      |		"mrzCheck": "OK"
      |	}
      |}""".stripMargin
}
