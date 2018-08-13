package com.snapswap.jumio.model

object JumioGenderEnum extends Enumeration {
  type Gender = Value

  val male, female = Value

  def parse(value: String): Gender = {
    value match {
      case "M" =>
        male
      case "F" =>
        female
    }
  }
}