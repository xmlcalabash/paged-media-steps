package com.xmlcalabash.paged.exceptions

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.QName

object PagedMediaException {
  val err_xc0204 = new QName("err", XProcConstants.ns_err, "XC0204")
  val err_xd0030 = new QName("err", XProcConstants.ns_err, "XD0030")

  def xdUnsupportedContentType(message: String): PagedMediaException = new PagedMediaException(err_xc0204, Some(message), None)
  def xdFormatterFailed(message: String, cause: Throwable): PagedMediaException = new PagedMediaException(err_xd0030, Some(message), Some(cause))
}

class PagedMediaException(override val code: QName,
                          override val message: Option[String],
                          val cause: Option[Throwable])
  extends XProcException(code, 1, message, None, List()) {

}
