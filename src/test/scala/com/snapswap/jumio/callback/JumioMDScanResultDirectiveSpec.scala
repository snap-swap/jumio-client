package com.snapswap.jumio.callback

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.snapswap.jumio.callback.JumioDirectives.jumioMDScanResult
import org.scalatest._

class JumioMDScanResultDirectiveSpec extends WordSpec with Matchers with Directives with ScalatestRouteTest {

  "jumioMDScanResult" should {
    "process 'uploaded' callback" in {
      Post("/", FormData.apply(Uri.Query.apply(Some(uploaded)))) ~> route ~> check {
        responseAs[String] shouldEqual "SUCCESS multi document callback [" +
          "scanReference=xxxxxxxx-xxxx-xxxx-xxxxxxxxxxxxxxxx, " +
          "firstName=N/A, " +
          "lastName=N/A, " +
          "name=N/A, " +
          "ssn=N/A, " +
          "signatureAvailable=N/A, " +
          "accountNumber=N/A, " +
          "issueDate=N/A, " +
          "address=[N/A]]"
      }
    }

    "process 'extracted' callback" in {
      Post("/", FormData.apply(Uri.Query.apply(Some(extracted)))) ~> route ~> check {
        responseAs[String] shouldEqual "SUCCESS multi document callback [" +
          "scanReference=xxxxxxxx-xxxx-xxxx-xxxxxxxxxxxxxxxx, " +
          "firstName=FIRSTNAME, " +
          "lastName=LASTNAME, " +
          "name=N/A, " +
          "ssn=xxxxxxxxx, " +
          "signatureAvailable=true, " +
          "accountNumber=N/A, " +
          "issueDate=N/A, " +
          "address=[N/A]]"
      }
    }
    "process 'discarded' callback" in {
      Post("/", FormData.apply(Uri.Query.apply(Some(discarded)))) ~> route ~> check {
        responseAs[String] shouldEqual "FAILURE multi document callback scanReference=xxxxxxxx-xxxx-xxxx-xxxxxxxxxxxxxxxx] with reason [Expected transaction status is 'EXTRACTED' or 'UPLOADED' but found 'DISCARDED']"
      }
    }
  }

  private val route: Route = jumioMDScanResult { scan =>
    complete(scan.toString)
  }

  private val uploaded = "timestamp=2014-10-20T12%3A06%3A49.016Z&scanReference=xxxxxxxx-xxxx-xxxx-xxxxxxxxxxxxxxxx&" +
    "document=%7B%22type%22%3A%22SSC%22%2C%22country%22%3A%22AUT%22%2C%22images%22%3A%5B%22https%3A%2F%2Fretrieval.netverify.com%2Fapi%2Fnetverify%2Fv2%2Fdocuments%2Fxxxxxxxx-xxxx-xxxx-xxxxxxxxxxxxxxxx%2Fpages%2F1%22%5D%2C%22status%22%3A%22UPLOADED%22%7D&" +
    "transaction=%7B%22customerId%22%3A%22CUSTOMERID%22%2C%22date%22%3A%222014-10-17T06%3A37%3A51.969Z%22%2C%22merchantScanReference%22%3A%22YOURSCANREFERENCE%22%2C%22source%22%3A%22DOC_API%22%2C%22status%22%3A%22DONE%22%7D"
  private val extracted = "timestamp=2014-10-20T12%3A06%3A49.016Z&scanReference=xxxxxxxx-xxxx-xxxx-xxxxxxxxxxxxxxxx&" +
    "document=%7B%22type%22%3A%22SSC%22%2C%22country%22%3A%22USA%22%2C%22extractedData%22%3A%7B%22firstName%22%3A%22FIRSTNAME%22%2C%22lastName%22%3A%22LASTNAME%22%2C%22signatureAvailable%22%3Atrue%2C%22ssn%22%3A%22xxxxxxxxx%22%7D%2C%22images%22%3A%5B%22https%3A%2F%2Fretrieval.netverify.com%2Fapi%2Fnetverify%2Fv2%2Fdocuments%2Fxxxxxxxx-xxxx-xxxx-xxxxxxxxxxxxxxxx%2Fpages%2F1%22%5D%2C%22status%22%3A%22EXTRACTED%22%7D&" +
    "transaction=%7B%22customerId%22%3A%22CUSTOMERID%22%2C%22date%22%3A%222014-10-17T06%3A37%3A51.969Z%22%2C%22merchantScanReference%22%3A%22YOURSCANREFERENCE%22%2C%22source%22%3A%22DOC_API%22%2C%22status%22%3A%22DONE%22%7D"
  private val discarded = "timestamp=2014-10-20T12%3A06%3A49.016Z&scanReference=xxxxxxxx-xxxx-xxxx-xxxxxxxxxxxxxxxx&" +
    "document=%7B%22type%22%3A%22SSC%22%2C%22country%22%3A%22USA%22%2C%22images%22%3A%5B%22https%3A%2F%2Fretrieval.netverify.com%2Fapi%2Fnetverify%2Fv2%2Fdocuments%2Fxxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx%2Fpages%2F1%22%5D%2C%22status%22%3A%22DISCARDED%22%7D&" +
    "transaction=%7B%22customerId%22%3A%22CUSTOMERID%22%2C%22date%22%3A%222014-10-17T06%3A37%3A51.969Z%22%2C%22merchantScanReference%22%3A%22YOURSCANREFERENCE%22%2C%22source%22%3A%22DOC_API%22%2C%22status%22%3A%22DONE%22%7D"
}