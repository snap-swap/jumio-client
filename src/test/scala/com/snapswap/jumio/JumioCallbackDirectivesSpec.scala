package com.snapswap.jumio

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._

class JumioCallbackDirectivesSpec extends WordSpecLike
  with Matchers
  with Directives
  with ScalatestRouteTest
  with JumioCallbackDirectives {

  "jumioScanResult" in {
    val route: Route = jumioScanResult { scan =>
      complete(scan.toString)
    }

    val scanRef = UUID.randomUUID()

    Post("/", approvedVerified(scanRef)) ~> route ~> check {
      responseAs[String] shouldEqual s"APPROVED_VERIFIED $scanRef (merchantScanID=xxxxxxxx, customerID=CUSTOMER) from SDK at IP 127.0.0.1, " +
        s"scanned at 2016-12-31T23:59:59.999Z, completed at 2017-12-31T23:59:59.999Z, " +
        s"checked [dataPositions=OK, documentValidation=OK, hologram=OK, mrzCode=OK, microprint=OK, securityFeatures=OK, signature=OK], " +
        s"identity verification similarity is MATCH, look like valid, " +
        s"URLs [idScanImage=https://lon.netverify.com/protected-image/yyyyyyyy, idScanImageBackside=N/A, idScanImageFace=https://lon.netverify.com/protected-image/zzzzzzzz]: " +
        s"USA PASSPORT of 'FIRSTNAME' 'LASTNAME', born '1990-01-01' resident of ',  , USA', 'P1234' number, 'N/A' personal number, expiry at '2022-12-31'"
    }

    Post("/", deniedFraud(scanRef)) ~> route ~> check {
      responseAs[String] shouldEqual s"DENIED_FRAUD $scanRef (merchantScanID=xxxxxxxx, customerID=N/A) from SDK at IP 127.0.0.1, " +
        s"scanned at N/A, completed at N/A, " +
        s"checked [dataPositions=N/A, documentValidation=N/A, hologram=N/A, mrzCode=N/A, microprint=N/A, securityFeatures=N/A, signature=N/A], " +
        s"URLs [idScanImage=https://lon.netverify.com/protected-image/yyyyyyyy, idScanImageBackside=N/A, idScanImageFace=N/A]: " +
        s"rejected as [100] MANIPULATED_DOCUMENT ([1001] PHOTO, [1004] DOB)"
    }
  }

  private def approvedVerified(scanRef: UUID) = FormData(
    "idExpiry" -> "2022-12-31",
    "idType" -> "PASSPORT",
    "idDob" -> "1990-01-01",
    "idCheckSignature" -> "OK",
    "idCheckDataPositions" -> "OK",
    "idCheckHologram" -> "OK",
    "idCheckMicroprint" -> "OK",
    "idCheckDocumentValidation" -> "OK",
    "idCountry" -> "USA",
    "idScanSource" -> "SDK",
    "idFirstName" -> "FIRSTNAME",
    "verificationStatus" -> "APPROVED_VERIFIED",
    "jumioIdScanReference" -> scanRef.toString,
    "personalNumber" -> "N/A",
    "merchantIdScanReference" -> "xxxxxxxx",
    "idCheckSecurityFeatures" -> "OK",
    "idCheckMRZcode" -> "OK",
    "idScanImage" -> "https://lon.netverify.com/protected-image/yyyyyyyy",
    "idScanImageFace" -> "https://lon.netverify.com/protected-image/zzzzzzzz",
    "callBackType" -> "NETVERIFYID",
    "clientIp" -> "127.0.0.1",
    "idLastName" -> "LASTNAME",
    "idAddress" -> """{"country":"USA", "stateCode":"US-OH"}""",
    "idScanStatus" -> "SUCCESS",
    "identityVerification" -> """{"similarity":"MATCH","validity":true}""",
    "idNumber" -> "P1234",
    "customerId" -> "CUSTOMER",
    "transactionDate" -> "2016-12-31T23:59:59.999Z",
    "callbackDate" -> "2017-12-31T23:59:59.999Z",
    "idType" -> "PASSPORT"
  )

  private def deniedFraud(scanRef: UUID) = FormData(
    "rejectReason" -> "{ \"rejectReasonCode\":\"100\", \"rejectReasonDescription\":\"MANIPULATED_DOCUMENT\", \"rejectReasonDetails\": [{ \"detailsCode\": \"1001\", \"detailsDescription\": \"PHOTO\" },{ \"detailsCode\": \"1004\", \"detailsDescription\": \"DOB\" }]}",
    "callBackType" -> "NETVERIFYID",
    "jumioIdScanReference" -> scanRef.toString,
    "verificationStatus" -> "DENIED_FRAUD",
    "idScanStatus" -> "ERROR",
    "idScanSource" -> "SDK",
    "idCheckDataPositions" -> "N/A",
    "idCheckDocumentValidation" -> "N/A",
    "idCheckHologram" -> "N/A",
    "idCheckMRZcode" -> "N/A",
    "idCheckMicroprint" -> "N/A",
    "idCheckSecurityFeatures" -> "N/A",
    "idCheckSignature" -> "N/A",
    "idType" -> "PASSPORT",
    "idCountry" -> "FRA",
    "idScanImage" -> "https://lon.netverify.com/protected-image/yyyyyyyy",
    "merchantIdScanReference" -> "xxxxxxxx",
    "clientIp" -> "127.0.0.1"
  )
}
