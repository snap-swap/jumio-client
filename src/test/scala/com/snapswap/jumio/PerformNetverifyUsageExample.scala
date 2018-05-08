package com.snapswap.jumio

import java.io.File
import java.nio.file.Files
import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.{ContentType, MediaTypes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.snapswap.jumio.http.AkkaHttpNetverifyClient
import com.snapswap.jumio.model.EnumJumioDocTypes
import com.snapswap.jumio.model.retrieval.JumioImageRawData


object PerformNetverifyUsageExample extends App {
  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  system.eventStream.setLogLevel(Logging.DebugLevel)

  val log = Logging(system, this.getClass)

  val client = new AkkaHttpNetverifyClient(
    clientToken = "",
    clientSecret = "",
    clientCompanyName = "snapswap",
    clientApplicationName = "remote-kyc",
    clientVersion = "v1",
    apiHost = "netverify.com"
  )


  def fileReader(fullPath: String): Source[ByteString, Any] = {
    Source.single(ByteString(Files.readAllBytes(new File(fullPath).toPath)))
  }

  val ref = s"test1-${LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)}"
  log.info(s"\n\nscan ref:$ref\n\n")

  val face = ""
  val front = ""
  val back = ""

  client.performNetverify(
    merchantIdScanReference = ref,
    country = "BEL",
    idType = EnumJumioDocTypes.id_card,
    callbackUrl = "https://dev-rkyc.snapswap.vc/callbacks/jumio",
    face = Some(JumioImageRawData(
      fileReader(face),
      ContentType(MediaTypes.`image/jpeg`)
    )),
    idFront = JumioImageRawData(
      fileReader(front),
      ContentType(MediaTypes.`image/jpeg`)
    ),
    idBack = Some(JumioImageRawData(
      fileReader(back),
      ContentType(MediaTypes.`image/jpeg`)
    ))
  ).flatMap { result =>
    log.info(s"\n\n$result\n\n")
    system.terminate()
  }.recoverWith {
    case ex =>
      log.error(ex, "\n\nsomething went wrong!\n\n")
      system.terminate()
  }
}
