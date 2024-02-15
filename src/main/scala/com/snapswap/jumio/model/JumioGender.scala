package com.snapswap.jumio.model

object JumioGenderEnum extends Enumeration {
  type Gender = Value

  val male, female = Value

  def parse(value: String): Option[Gender] = {
    value match {
      case "M" => Some(male)
      case "F" => Some(female)
      case _ => None
    }
  }
}