package com.snapswap.jumio

import java.time.{LocalDate, ZoneOffset}

import com.snapswap.jumio.json.protocol.JumioUnmarshaller
import com.snapswap.jumio.model._
import com.snapswap.jumio.model.netverify.{AcceptedIdDocs, JumioRejectReason, JumioRejection}
import com.snapswap.jumio.model.retrieval.{JumioImage, JumioImagesInfo, JumioScan}
import org.scalatest._
import spray.json._

class UnmarshallerSpec extends WordSpecLike with Matchers with JumioUnmarshaller {

  "Unmarshaller" should {
    "parse acceptedIdTypes response" in {
      import EnumJumioDocTypes._
      val result = acceptedIdDocs.parseJson.convertTo[AcceptedIdDocs]
      result shouldBe Map(
        "HRV" -> Seq(id_card -> true, driving_license -> true, passport -> false),
        "MOZ" -> Seq(passport -> false)
      )
    }
    "parse details of PENDING scan " in {
      val result = pendingScan.parseJson.convertTo[JumioScan]
      result.transaction.status shouldBe EnumJumioTxStatuses.pending
      result.transaction.source shouldBe EnumJumioSources.sdk
      result.transaction.date shouldBe Some(LocalDate.of(2016, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime.toZonedDateTime)
    }
    "parse details of success scan with identity verification " in {
      val result = doneScanWithIdentityVerification.parseJson.convertTo[JumioScan]
      result.transaction.status shouldBe EnumJumioTxStatuses.done
      result.transaction.source shouldBe EnumJumioSources.sdk
    }
    "parse JumioImages with single image as JsObject" in {
      val result = jumioImagesWithObject.parseJson.convertTo[JumioImagesInfo]
      result.timestamp shouldBe "2018-01-01T00:00:00.000Z"
      result.scanReference shouldBe "df4d7b53-ad7a-4ab4-9eef-e6b099ec20bc"
      result.images.head shouldBe a[JumioImage]
    }
    "parse JumioImages with multiple images as JsArray" in {
      val result = jumioImagesWithArray.parseJson.convertTo[JumioImagesInfo]
      result.timestamp shouldBe "2018-01-01T00:00:00.000Z"
      result.scanReference shouldBe "df4d7b53-ad7a-4ab4-9eef-e6b099ec20bc"
      result.images.head shouldBe a[JumioImage]
    }
    "parse JumioImage" in {
      val result = jumioFrontImage.parseJson.convertTo[JumioImage]
      result.classifier shouldBe "front"
      result.href shouldBe "https://netverify.com/api/netverify/v2/scans/df4d7b53-ad7a-4ab4-9eef-e6b099ec20bc/images/front"
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

  val jumioFrontImage =
    """{
      |"classifier":"front",
      |"href":"https://netverify.com/api/netverify/v2/scans/df4d7b53-ad7a-4ab4-9eef-e6b099ec20bc/images/front"
      |}""".stripMargin

  val jumioFaceImage =
    """{
      |"classifier":"face",
      |"href":"https://netverify.com/api/netverify/v2/scans/df4d7b53-ad7a-4ab4-9eef-e6b099ec20bc/images/face"
      |}""".stripMargin

  val jumioImagesWithArray =
    s"""{
       |"timestamp":"2018-01-01T00:00:00.000Z",
       |"images":[$jumioFrontImage,$jumioFaceImage],
       |"scanReference":"df4d7b53-ad7a-4ab4-9eef-e6b099ec20bc"
       |}""".stripMargin

  val jumioImagesWithObject =
    s"""{
       |  "timestamp":"2018-01-01T00:00:00.000Z",
       |  "images":$jumioFrontImage,
       |  "scanReference":"df4d7b53-ad7a-4ab4-9eef-e6b099ec20bc"
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

  val acceptedIdDocs =
    """{
      |    "timestamp": "2018-05-16T11:03:05.748Z",
      |    "acceptedIdTypes": [
      |        {
      |            "countryCode": "HRV",
      |            "countryName": "Croatia",
      |            "idTypes": [
      |                {
      |                    "acquisitionConfig": {
      |                        "backSide": true
      |                    },
      |                    "idType": "ID_CARD"
      |                },
      |                {
      |                    "acquisitionConfig": {
      |                        "backSide": true
      |                    },
      |                    "idType": "DRIVING_LICENSE"
      |                },
      |                {
      |                    "acquisitionConfig": {
      |                        "backSide": false
      |                    },
      |                    "idType": "PASSPORT"
      |                }
      |            ]
      |        },
      |        {
      |            "countryCode": "MOZ",
      |            "countryName": "Mozambique",
      |            "idTypes": [
      |                {
      |                    "acquisitionConfig": {
      |                        "backSide": false
      |                    },
      |                    "idType": "PASSPORT"
      |                }
      |            ]
      |        }
      |    ]
      |}""".stripMargin
}
