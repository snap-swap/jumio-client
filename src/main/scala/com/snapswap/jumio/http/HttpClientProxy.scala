package com.snapswap.jumio.http

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.HttpRequest
import com.snapswap.jumio.http.HttpClient.WrappedResponse


case object HttpClientProxy {
  def props(httpClient: ActorRef): Props =
    Props(new HttpClientProxy(httpClient))
}

class HttpClientProxy(httpClient: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case r: HttpRequest =>
      log.debug(s"Request to ${r.method.value} ${r.uri} will be send via streaming client")
      httpClient ! r -> HttpClient.ProxyMeta(self, sender(), r)
    case w@WrappedResponse(_, HttpClient.ProxyMeta(_, pipeTo, _)) =>
      pipeTo ! w
  }
}
