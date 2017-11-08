package com.snapswap.jumio

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest._
import spray.json._

class UnmarshallerSpec extends WordSpec with Matchers {

  import unmarshaller._

  "Unmarshaller" should {
    "parse details of PENDING scan " in {
      val result = pendingScan.parseJson.convertTo[JumioScan]
      result.transaction.status shouldBe EnumJumioTxStatuses.pending
      result.transaction.source shouldBe EnumJumioSources.sdk
      result.transaction.date shouldBe Some(DateTime.now(DateTimeZone.UTC).withDate(2016, 1, 1).withTime(0, 0, 0, 0))
    }
    "parse details of success scan with identity verification " in {
      val result = doneScanWithIdentityVerification.parseJson.convertTo[JumioScan]
      result.transaction.status shouldBe EnumJumioTxStatuses.done
      result.transaction.source shouldBe EnumJumioSources.sdk
    }
    "parse JumioImagesInfo" in {
      val result = jumioImagesInfo.parseJson.convertTo[JumioImagesInfo]
      result.timestamp shouldBe "2017-06-08T05:41:22.294+0000"
      result.scanReference shouldBe "e6c76aa8-e2c2-479e-a586-b5380dc6f14a"
      result.images.head shouldBe a[JumioImage]
    }
    "parse JumioImage" in {
      val result = jumioImage.parseJson.convertTo[JumioImage]
      result.classifier shouldBe "1"
      result.href shouldBe "https://retrieval.netverify.com/api/netverify/v2/documents/e6c76aa8-e2c2-479e-a586-b5380dc6f14a/pages/1"
      result.maskhint shouldBe None
    }
    "parse JumioRejectReason" when {
      "rejectReasonDetails presented as an array" in {
        val result = jumioRejectReason.arrayDetails(jumioRejection).parseJson.convertTo[JumioRejectReason]
        result.details shouldBe Seq(jumioRejection)
      }
      "rejectReasonDetails presented as an empty array" in {
        val result = jumioRejectReason.emptyArrayDetails().parseJson.convertTo[JumioRejectReason]
        result.details shouldBe Seq.empty
      }
      "rejectReasonDetails presented as a single object" in {
        val result = jumioRejectReason.objectDetails(jumioRejection).parseJson.convertTo[JumioRejectReason]
        result.details shouldBe Seq(jumioRejection)
      }
      "rejectReasonDetails isn't presented" in {
        val result = jumioRejectReason.detailsNotPresented().parseJson.convertTo[JumioRejectReason]
        result.details shouldBe Seq.empty
      }
    }
  }


  val jumioRejection = JumioRejection("__code__", "__description__")

  object jumioRejectReason {
    def arrayDetails(rejection: JumioRejection) = body(s""","rejectReasonDetails":[${rejection.toJson.prettyPrint}]""")

    def emptyArrayDetails() = body(s""","rejectReasonDetails":[]""")

    def objectDetails(rejection: JumioRejection) = body(s""","rejectReasonDetails":${rejection.toJson.prettyPrint}""")

    def detailsNotPresented() = body("")

    private def body(details: String) =
      s"""{
         |"rejectReasonCode":"__reject_reason_code__",
         |"rejectReasonDescription":"__reject_reason_description__"
         |$details
         |}""".stripMargin
  }

  val jumioImage =
    """{
      |"classifier":1,
      |"href":"https://retrieval.netverify.com/api/netverify/v2/documents/e6c76aa8-e2c2-479e-a586-b5380dc6f14a/pages/1"
      |}""".stripMargin

  val jumioImagesInfo =
    s"""{
       |"timestamp":"2017-06-08T05:41:22.294+0000",
       |"images":[$jumioImage],
       |"scanReference":"e6c76aa8-e2c2-479e-a586-b5380dc6f14a"
       |}""".stripMargin

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

  val doneScanWithIdentityVerification =
    """{
      |  "timestamp": "2017-11-08T08:33:29.031Z",
      |  "document": {
      |    "number": "730148999",
      |    "dob": "1987-04-28",
      |    "lastName": "MOROZOV",
      |    "firstName": "VLADIMIR",
      |    "status": "APPROVED_VERIFIED",
      |    "issuingCountry": "RUS",
      |    "issuingDate": "2000-02-19",
      |    "type": "PASSPORT",
      |    "expiry": "2003-02-19"
      |  },
      |  "verification": {
      |    "faceMatch": "92",
      |    "identityVerification": {
      |      "similarity": "MATCH",
      |      "validity": "true"
      |    },
      |    "mrzCheck": "OK"
      |  },
      |  "scanReference": "22eee721-c876-4a35-9900-9b6675f7de60",
      |  "transaction": {
      |    "clientIp": "2.93.239.137",
      |    "source": "SDK",
      |    "date": "2017-11-08T07:07:36.902Z",
      |    "status": "DONE",
      |    "merchantScanReference": "DF5AF751-7AAA-45B9-A62D-9B1D5271A59B"
      |  }
      |}""".stripMargin
}
