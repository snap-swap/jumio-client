package com.snapswap.jumio

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.snapswap.jumio.model.{EnumJumioSources, JumioRawResult}
import org.scalatest._

class JumioCallbackDirectivesSpec extends WordSpecLike
  with Matchers
  with Directives
  with ScalatestRouteTest
  with JumioCallbackDirectives
  with OneInstancePerTest {


  "JumioCallbackDirectives" should {
    "parse callback into a JumioRawResult with the minimal set of data" in {
      Post("/", FormData(callbackData)) ~> jumioScanResult(r => complete(r.toString)) ~> check{
        responseAs[String] shouldBe JumioRawResult(
          merchantScanReference = callbackData("merchantIdScanReference"),
          scanReference = scanRef.toString,
          EnumJumioSources.sdk,
          rawData = callbackData
        ).toString
      }
    }
  }

  private lazy val scanRef = UUID.randomUUID()
  private lazy val callbackData: Map[String, String] = Map(
    "idScanSource" -> "SDK",
    "jumioIdScanReference" -> scanRef.toString,
    "merchantIdScanReference" -> "xxxxxxxx",
    "unknownField" -> "unknownValue"
  )
}
