package com.snapswap.jumio

import akka.actor.ActorSystem
import akka.event.Logging

import scala.concurrent.Future

object RetrieveImageUsageExample extends App {

  import java.nio.file.Paths

  import akka.stream.scaladsl.FileIO
  import akka.stream.{ActorMaterializer, IOResult}


  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  val log = Logging(system, this.getClass)

  //use retrieval api credentials here
  val client = new AkkaHttpJumioClient(
    clientToken = "",
    clientSecret = "",
    clientCompanyName = "snapswap",
    clientApplicationName = "remote-kyc",
    clientVersion = "v1",
    apiHost = "netverify.com"
  )


  def fileWriter(bss: RawImage, path: String): Future[IOResult] =
    bss.data.runWith(FileIO.toPath(Paths.get(path)))

  def saveImagesForJumioScan(scanReference: String, dir: String)
                            (getImageInfoMethod: String => Future[JumioImagesInfo],
                             obtainImageMethod: String => Future[RawImage]): Future[Unit] = (for {
    scan <- getImageInfoMethod(scanReference)
    doneImages = scan.images.map { case JumioImage(classifier, href, _) =>
      obtainImageMethod(href).flatMap { response =>
        val fileName = s"$dir$scanReference.$classifier.jpg"
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

  val scanReference = "b4aa577c-c46f-42fd-9c2e-e68bb44d49d4"
  val mdScanReference = "061e452d-311a-47cb-b4b4-c4af3a03b6ed"

  val getFiles = saveImagesForJumioScan(scanReference, "")(client.scanImages, client.obtainImage)
  val getMdFiles = saveImagesForJumioScan(mdScanReference, "")(client.mdScanImages, client.obtainMdImage)

  for {
    _ <- getFiles
    _ <- getMdFiles
    _ <- system.terminate()
  } yield ()
}