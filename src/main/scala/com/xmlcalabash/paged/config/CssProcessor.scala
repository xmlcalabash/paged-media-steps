package com.xmlcalabash.paged.config

import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcMetadata}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

import java.io.OutputStream

trait CssProcessor {
  def initialize(runtime: XMLCalabashRuntime, options: Map[QName,XdmValue]): Unit
  def addStylesheet(doc: XdmNode, meta: XProcMetadata): Unit
  def addStylesheet(uri: String): Unit
  def format(doc: XdmNode, contentType: MediaType, out: OutputStream): Unit
}
