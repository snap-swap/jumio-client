package com.snapswap.jumio.json.protocol

import spray.json._

private[protocol] trait JumioJsonReader[T] extends RootJsonFormat[T] {
  override def write(obj: T): JsValue =
    serializationError(s"object ${obj.getClass.getSimpleName} can't be formatted into Json")
}
