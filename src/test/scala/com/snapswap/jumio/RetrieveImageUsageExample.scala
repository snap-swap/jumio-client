package com.snapswap.jumio

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.MediaTypes
import com.snapswap.jumio.http.AkkaHttpRetrievalClient
import com.snapswap.jumio.model.retrieval.{JumioImage, JumioImageRawData, JumioImagesInfo}

import scala.concurrent.Future

object RetrieveImageUsageExample extends App {

  import java.nio.file.Paths

  import akka.stream.scaladsl.FileIO
  import akka.stream.{ActorMaterializer, IOResult}


  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  val log = Logging(system, this.getClass)

  val client = new AkkaHttpRetrievalClient(
    clientToken = "",
    clientSecret = "",
    clientCompanyName = "snapswap",
    clientApplicationName = "remote-kyc",
    clientVersion = "v1",
    apiHost = "netverify.com"
  )


  def fileWriter(bss: JumioImageRawData, path: String): Future[IOResult] = {
    val extension = bss.contentType.mediaType match {
      case MediaTypes.`image/jpeg` =>
        "jpg"
      case MediaTypes.`image/png` =>
        "png"
      case _ =>
        ""
    }
    bss.data.runWith(FileIO.toPath(Paths.get(s"$path.$extension")))
  }

  def saveImagesForJumioScan(scanReference: String, dir: String)
                            (getImageInfoMethod: String => Future[JumioImagesInfo],
                             obtainImageMethod: String => Future[JumioImageRawData]): Future[Unit] = (for {
    scan <- getImageInfoMethod(scanReference)
    doneImages = scan.images.map { case JumioImage(classifier, href, _) =>
      obtainImageMethod(href).flatMap { response =>
        val fileName = s"$dir$scanReference.$classifier"
        fileWriter(response, fileName).map { ioResult =>
          (fileName, ioResult)
        }
      }
    }
    result <- Future.sequence(doneImages)
  } yield result.foreach {
    case (f, r) if r.wasSuccessful =>
      log.info(s"scan image for scanReference $scanReference was saved successfully as $f")
    case _ =>
      log.error(s"!!! ATTENTION !!! there are some problems during saving scan for scanReference $scanReference")
  }).recover {
    case ex =>
      Future.successful(log.error(s"${ex.getClass.getSimpleName}: ${ex.getMessage}"))
  }

  val scanReference = "5577a6aa-11d5-4d3f-82f9-82df075e191d"
  val mdScanReference = "939f45cb-30e4-4028-80c1-48a02d6078a4"

  val getFiles = saveImagesForJumioScan(scanReference, "")(client.scanImages, client.obtainImage)
  val getMdFiles = saveImagesForJumioScan(mdScanReference, "")(client.mdScanImages, client.obtainMdImage)

  for {
    _ <- getFiles
    _ <- getMdFiles
    _ <- system.terminate()
  } yield ()
}