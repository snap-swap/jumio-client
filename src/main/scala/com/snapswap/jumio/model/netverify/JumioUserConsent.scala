package com.snapswap.jumio.model.netverify

import com.snapswap.jumio.model.netverify.JumioUserConsent.ConsentStatusEnum.ConsentStatus
import com.snapswap.jumio.model.netverify.JumioUserConsent.{UserConsent, UserLocation}

import java.time.ZonedDateTime

case class JumioUserConsent(userIp: String, userLocation: UserLocation, consent: Option[UserConsent])

object JumioUserConsent {
  case class UserLocation(country: String, state: Option[String])

  case class UserConsent(obtained: ConsentStatus, obtainedAt: ZonedDateTime)

  object ConsentStatusEnum extends Enumeration {
    type ConsentStatus = Value

    val yes, no, na: ConsentStatus = Value
  }

  def positiveConsent(userIp: String,
                      country: String,
                      state: Option[String],
                      obtainedAt: ZonedDateTime): JumioUserConsent = JumioUserConsent(
    userIp = userIp,
    userLocation = UserLocation(country = country, state = state),
    consent = Some(UserConsent(obtained = ConsentStatusEnum.yes, obtainedAt = obtainedAt))
  )

  def noConsent(userIp: String,
                country: String): JumioUserConsent = JumioUserConsent(
    userIp = userIp,
    userLocation = UserLocation(country = country, state = None),
    consent = None
  )
}