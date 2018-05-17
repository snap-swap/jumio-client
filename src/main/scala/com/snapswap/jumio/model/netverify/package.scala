package com.snapswap.jumio.model

import com.snapswap.jumio.model.EnumJumioDocTypes.JumioDocType

package object netverify {
  type AcceptedIdDocs = Map[Country, Seq[(JumioDocType, HasBackSide)]]
  type Country = String
  type HasBackSide = Boolean
}
