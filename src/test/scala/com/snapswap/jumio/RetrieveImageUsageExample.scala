package com.snapswap.jumio

import java.io.FileOutputStream

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.MediaTypes
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.snapswap.jumio.http.{AkkaHttpRetrievalClient, JumioRetrievalConnectionParams}
import com.snapswap.jumio.model.retrieval.{JumioImage, JumioImageRawData, JumioImagesInfo}

import scala.concurrent.Future

object RetrieveImageUsageExample extends App {

  import akka.stream.ActorMaterializer


  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  val log = Logging(system, this.getClass)

  val client = new AkkaHttpRetrievalClient(
    clientCompanyName = "snapswap",
    clientApplicationName = "remote-kyc",
    clientVersion = "v1"
  )

  implicit val connectionParams = JumioRetrievalConnectionParams(
    apiHost = "https://netverify.com",
    token = "",
    secret = ""
  )

  def saveImagesForJumioScan(scanReference: String, dir: String)
                            (getImageInfoMethod: String => Future[JumioImagesInfo],
                             obtainImageMethod: Seq[JumioImage] => Source[(JumioImageRawData, JumioImage), Any]): Future[Unit] =
    (for {
      scan <- getImageInfoMethod(scanReference)
      result <- obtainImageMethod(scan.images)
        .map { case (JumioImageRawData(data, contentType), JumioImage(classifier, _, _)) =>
          //prepare for saving
          val extension = contentType.mediaType match {
            case MediaTypes.`image/jpeg` =>
              "jpg"
            case MediaTypes.`image/png` =>
              "png"
            case _ =>
              ""
          }
          val fileName = s"$dir$scanReference.$classifier.$extension"
          fileName -> data
        }
        .map { case (fileName, data) =>
          //save file
          data.fold(ByteString.createBuilder) { case (builder, bs) =>
            builder.append(bs)
          }.map { builder =>
            val file = new FileOutputStream(fileName)
            file.write(builder.result().toArray)
            file.close()
            log.info(s"scan image for scanReference $scanReference was saved successfully as $fileName")
          }
        }
        .flatMapConcat(f => f)
        .runWith(Sink.ignore)
    } yield ()).recover {
      case ex =>
        Future.successful(log.error(s"${ex.getClass.getSimpleName}: ${ex.getMessage}"))
    }

  val scanReference = "5577a6aa-11d5-4d3f-82f9-82df075e191d"

  for {
    _ <- saveImagesForJumioScan(scanReference, "")(client.scanImages, client.obtainImage)
    _ <- system.terminate()
  } yield ()
}