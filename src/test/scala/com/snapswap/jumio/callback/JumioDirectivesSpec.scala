package com.snapswap.jumio.callback

import java.util.UUID
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.model._
import org.scalatest._
import JumioDirectives.jumioScanResult

class JumioDirectivesSpec extends WordSpec with Matchers with Directives with ScalatestRouteTest {

  "jumioScanResult" in {
    val route: Route = jumioScanResult { scan =>
      complete(scan.toString)
    }

    val scanRef = UUID.randomUUID()

    Post("/", approvedVerified(scanRef)) ~> route ~> check {
      responseAs[String] shouldEqual s"APPROVED_VERIFIED $scanRef (merchantScanID=xxxxxxxx, customerID=CUSTOMER) from SDK at IP 127.0.0.1, " +
        s"scanned at 2016-12-31T23:59:59.999Z, completed at 2017-12-31T23:59:59.999Z, " +
        s"checked [dataPositions=OK, documentValidation=OK, hologram=OK, mrzCode=OK, microprint=OK, securityFeatures=OK, signature=OK], " +
        s"94% faceMatch (looks alive), " +
        s"URLs [idScanImage=https://lon.netverify.com/protected-image/yyyyyyyy, idScanImageBackside=N/A, idScanImageFace=https://lon.netverify.com/protected-image/zzzzzzzz]: " +
        s"FRA PASSPORT of 'JOHN' 'DOE', born '1976-12-31', '10CL12345' number, 'N/A' personal number, expiry at '2020-12-31'"
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
    "callBackType" -> "NETVERIFYID",
    "jumioIdScanReference" -> scanRef.toString,
    "verificationStatus" -> "APPROVED_VERIFIED",
    "idScanStatus" -> "SUCCESS",
    "idScanSource" -> "SDK",
    "idCheckDataPositions" -> "OK",
    "idCheckDocumentValidation" -> "OK",
    "idCheckHologram" -> "OK",
    "idCheckMRZcode" -> "OK",
    "idCheckMicroprint" -> "OK",
    "idCheckSecurityFeatures" -> "OK",
    "idCheckSignature" -> "OK",
    "transactionDate" -> "2016-12-31T23:59:59.999Z",
    "callbackDate" -> "2017-12-31T23:59:59.999Z",
    "idType" -> "PASSPORT",
    "idCountry" -> "FRA",
    "idFaceMatch" -> "94",
    "idScanImage" -> "https://lon.netverify.com/protected-image/yyyyyyyy",
    "idScanImageFace" -> "https://lon.netverify.com/protected-image/zzzzzzzz",
    "idNumber" -> "10CL12345",
    "idFirstName" -> "JOHN",
    "idLastName" -> "DOE",
    "idDob" -> "1976-12-31",
    "idExpiry" -> "2020-12-31",
    "personalNumber" -> "N/A",
    "merchantIdScanReference" -> "xxxxxxxx",
    "customerId" -> "CUSTOMER",
    "clientIp" -> "127.0.0.1",
    "idFaceLiveness" -> "TRUE"
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
