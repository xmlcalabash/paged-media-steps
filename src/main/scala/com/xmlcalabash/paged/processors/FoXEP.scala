package com.xmlcalabash.paged.processors

import com.renderx.xep.{FOTarget, FormatterImpl}
import com.xmlcalabash.paged.config.FoProcessor
import com.xmlcalabash.paged.exceptions.PagedMediaException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.{MediaType, S9Api}
import jp.co.antenna.XfoJavaCtl.{MessageListener, XfoFormatPageListener, XfoObj}
import net.sf.saxon.s9api.{QName, Serializer, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, OutputStream}
import java.net.URI
import java.util.Properties
import javax.xml.transform.sax.SAXSource
import scala.collection.mutable

object FoXEP {
  private val _formatMap = mutable.HashMap.empty[MediaType,String]
  _formatMap.put(new MediaType("application", "pdf"), "PDF")
  _formatMap.put(new MediaType("application", "postscript"), "PostScript")
  _formatMap.put(new MediaType("application", "afp"), "AFP")

  def formatMap: Map[MediaType,String] = _formatMap.toMap
}


class FoXEP extends FoProcessor {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var runtime: XMLCalabashRuntime = _
  private var options: Map[QName,XdmValue] = _
  private val properties = new Properties()

  private var xep: FormatterImpl = _

  override def initialize(runtime: XMLCalabashRuntime, baseURI: URI, options: Map[QName, XdmValue]): Unit = {
    this.runtime = runtime
    this.options = options

    for ((name, value) <- options) {
      properties.put(name.getLocalName, value.getUnderlyingValue.getStringValue)
    }

    xep = new FormatterImpl(properties, new FoLogger())
  }

  override def format(doc: XdmNode, contentType: MediaType, out: OutputStream): Unit = {
    val outputFormat = if (FoAH.formatMap.contains(contentType)) {
      FoAH.formatMap(contentType)
    } else {
      throw PagedMediaException.xdUnsupportedContentType(s"Unsupported content type: ${contentType}")
    }

    val fodoc = S9Api.xdmToInputSource(runtime.config, doc)
    val source = new SAXSource(fodoc)
    xep.render(source, new FOTarget(out, outputFormat))
  }

  private class FoLogger extends com.renderx.xep.lib.Logger {
    override def openDocument(): Unit = {
      logger.info("Xep processing begins")
    }

    override def closeDocument(): Unit = {
      logger.info("Xep processing ends")
    }

    override def event(name: String, message: String): Unit = {
      logger.debug(s"Xep ${name}: ${message}")
    }

    override def openState(state: String): Unit = {
      logger.debug(s"Xep process starts: ${state}")
    }

    override def closeState(state: String): Unit = {
      logger.debug(s"Xep process ends: ${state}")
    }

    override def info(message: String): Unit = {
      logger.info(message)
    }

    override def warning(message: String): Unit = {
      logger.warn(message)
    }

    override def error(message: String): Unit = {
      logger.error(message)
    }

    override def exception(message: String, ex: Exception): Unit = {
      logger.error(message)
      throw ex
    }
  }
}
