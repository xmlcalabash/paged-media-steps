package com.xmlcalabash.paged.config

import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

import java.io.OutputStream
import java.net.URI

trait FoProcessor {
  def initialize(runtime: XMLCalabashRuntime, baseURI: URI, options: Map[QName,XdmValue]): Unit
  def format(doc: XdmNode, contentType: MediaType, out: OutputStream): Unit
}
