package com.snapswap.jumio

import java.time.LocalDate
import java.util.UUID

import com.snapswap.jumio.model.JumioRawResult
import com.snapswap.jumio.model.netverify.{JumioScanResult, JumioScanSuccess}
import org.scalatest._

class JumioScanResultSpec extends WordSpecLike with Matchers {

  val scanRef = UUID.randomUUID()

  "JumioScanResult" should {
    "correctly parse success callback" in {
      JumioScanResult.of(approvedVerifiedResult(scanRef)).toString shouldBe
        s"APPROVED_VERIFIED $scanRef (merchantScanID=xxxxxxxx, customerID=CUSTOMER) from SDK at IP 127.0.0.1, " +
          s"scanned at 2016-12-31T23:59:59.999Z, completed at 2017-12-31T23:59:59.999Z, " +
          s"checked [dataPositions=OK, documentValidation=OK, hologram=OK, mrzCode=OK, microprint=OK, securityFeatures=OK, signature=OK], " +
          s"identity verification similarity is MATCH, look like valid, " +
          s"URLs [idScanImage=https://lon.netverify.com/protected-image/yyyyyyyy, idScanImageBackside=N/A, idScanImageFace=https://lon.netverify.com/protected-image/zzzzzzzz]: " +
          s"USA PASSPORT of 'FIRSTNAME' 'LASTNAME', born '1990-01-01' resident of ',  , USA', 'P1234' number, 'N/A' personal number, expiry at '2022-12-31'"
    }
    "correctly parse success fraud callback" in {
      JumioScanResult.of(deniedFraud(scanRef)).toString shouldBe
        s"DENIED_FRAUD $scanRef (merchantScanID=xxxxxxxx, customerID=N/A) from SDK at IP 127.0.0.1, " +
          s"scanned at N/A, completed at N/A, " +
          s"checked [dataPositions=N/A, documentValidation=N/A, hologram=N/A, mrzCode=N/A, microprint=N/A, securityFeatures=N/A, signature=N/A], " +
          s"URLs [idScanImage=https://lon.netverify.com/protected-image/yyyyyyyy, idScanImageBackside=N/A, idScanImageFace=N/A]: " +
          s"rejected as [100] MANIPULATED_DOCUMENT ([1001] PHOTO, [1004] DOB)"
    }
    "correct parse issuingDate from success callback" in {
      JumioScanResult.of(withIssueDate(scanRef)) match {
        case success: JumioScanSuccess =>
          success.document.issueDate.map(_.toLocalDate) shouldBe Some(LocalDate.parse("2014-01-16"))
        case other =>
          other shouldBe a[JumioScanSuccess]
      }
    }
  }


  private def approvedVerifiedResult(scanRef: UUID): JumioRawResult = JumioRawResult.of(Map(
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
  ))

  private def deniedFraud(scanRef: UUID): JumioRawResult = JumioRawResult.of(Map(
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
  ))

  private def withIssueDate(scanRef: UUID): JumioRawResult = JumioRawResult.of(Map(
    "dCheckMicroprint" -> "OK",
    "callBackType" -> "NETVERIFYID",
    "issuingDate" -> "2014-01-16",
    "merchantIdScanReference" -> "70aa27d8-7521-11e8-9530-47c12500f263",
    "idCheckDocumentValidation" -> "OK",
    "idLastName" -> "LICIS",
    "callbackDate" -> "2018-06-21T07:08:34.119Z",
    "idNumber" -> "PA0384583",
    "idCheckDataPositions" -> "OK",
    "customerId" -> "f06a6de5-7520-11e8-9530-9b16c8d47417",
    "idScanImageFace" -> "https://netverify.com/recognition/v1/idscan/86bb2356-5c5d-42a0-9aa2-32ee4867de1c/face",
    "idFirstName" -> "VALDIS",
    "firstAttemptDate" -> "2018-06-21T07:06:48.163Z",
    "idScanSource" -> "SDK",
    "idCheckMRZcode" -> "OK",
    "idScanStatus" -> "SUCCESS",
    "idType" -> "ID_CARD",
    "jumioIdScanReference" -> "86bb2356-5c5d-42a0-9aa2-32ee4867de1c",
    "verificationStatus" -> "APPROVED_VERIFIED",
    "idScanImage" -> "https://netverify.com/recognition/v1/idscan/86bb2356-5c5d-42a0-9aa2-32ee4867de1c/front",
    "identityVerification" -> """{"similarity":"MATCH","validity":true}""",
    "idDob" -> "1988-05-07",
    "personalNumber" -> "N/A",
    "transactionDate" -> "2018-06-21T07:06:14.648Z",
    "idCountry" -> "LVA",
    "optionalData1" -> "070588 12215",
    "idScanImageBackside" -> "https://netverify.com/recognition/v1/idscan/86bb2356-5c5d-42a0-9aa2-32ee4867de1c/back",
    "idSubtype" -> "NATIONAL_ID",
    "idCheckSignature" -> "OK",
    "idExpiry" -> "2019-01-15",
    "clientIp" -> "212.3.196.199",
    "idCheckSecurityFeatures" -> "OK",
    "idCheckHologram" -> "OK"
  ))
}
