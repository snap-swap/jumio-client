package com.snapswap.jumio.json.protocol

import com.snapswap.jumio.model.{EnumJumioAddressFormats, JumioAddress}
import spray.json._


trait JumioAddressUnmarshaller {
  self: DefaultJsonProtocol =>

  private case class USAddress(city: Option[String],
                               stateCode: Option[String],
                               streetName: Option[String],
                               streetSuffix: Option[String],
                               streetDirection: Option[String],
                               streetNumber: Option[String],
                               unitDesignator: Option[String],
                               unitNumber: Option[String],
                               zip: Option[String],
                               zipExtension: Option[String],
                               country: Option[String]) {
    def toAddress = JumioAddress(
      country = country.getOrElse(""),
      region = stateCode,
      city = city.getOrElse(""),
      postalCode = zip.map(v => s" $v").getOrElse("") + zipExtension.map(v => s"-$v").getOrElse(""),
      streetAddress = Seq(
        streetName,
        streetSuffix.map(v => s" $v"),
        streetDirection.map(v => s" $v"),
        streetNumber.map(v => s" $v"),
        unitNumber.map(num => ", " + unitDesignator.map(v => s"$v ").getOrElse("") + num)
      ).flatten.mkString,
      format = EnumJumioAddressFormats.us)
  }

  private case class EUAddress(city: Option[String],
                               province: Option[String],
                               streetName: Option[String],
                               streetNumber: Option[String],
                               unitDetails: Option[String],
                               postalCode: Option[String],
                               country: Option[String]) {
    def toAddress = JumioAddress(
      country = country.getOrElse(""),
      region = province,
      city = city.getOrElse(""),
      postalCode = postalCode.getOrElse(""),
      streetAddress = Seq(
        streetName,
        streetNumber.map(v => s" $v"),
        unitDetails.map(v => s", $v")
      ).flatten.mkString,
      format = EnumJumioAddressFormats.eu)
  }

  private case class RawAddress(line1: String,
                                line2: Option[String],
                                line3: Option[String],
                                line4: Option[String],
                                line5: Option[String],
                                country: Option[String],
                                postalCode: Option[String],
                                city: Option[String]) {
    def toAddress = JumioAddress(
      country = country.getOrElse(""),
      region = None,
      city = city.getOrElse(""),
      postalCode = postalCode.getOrElse(""),
      streetAddress = Seq(Some(line1), line2, line3, line4, line5).flatten.map(_.trim).mkString(" "),
      format = EnumJumioAddressFormats.raw)
  }

  private implicit val euAddressFormat = jsonFormat7(EUAddress)
  private implicit val rawAddressFormat = jsonFormat8(RawAddress)
  private implicit val usAddressFormat = jsonFormat11(USAddress)

  implicit object JumioAddressFormat extends JumioJsonReader[JumioAddress] {
    override def read(json: JsValue): JumioAddress = json match {
      case obj: JsObject =>
        (obj.fields.get("stateCode"), obj.fields.get("line1")) match {
          case (Some(_), None) =>
            usAddressFormat.read(json).toAddress
          case (None, Some(_)) =>
            rawAddressFormat.read(json).toAddress
          case _ =>
            euAddressFormat.read(json).toAddress
        }
      case x =>
        deserializationError("Expected address as object, but got " + x)
    }
  }

}
