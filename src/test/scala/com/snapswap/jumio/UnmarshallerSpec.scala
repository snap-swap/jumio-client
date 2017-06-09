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
      result.transaction.date shouldBe DateTime.now(DateTimeZone.UTC).withDate(2016, 1, 1).withTime(0, 0, 0, 0)
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
}
