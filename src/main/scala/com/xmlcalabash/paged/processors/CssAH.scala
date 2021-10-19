package com.xmlcalabash.paged.processors

import com.xmlcalabash.paged.config.{CssProcessor, FoProcessor}
import com.xmlcalabash.paged.exceptions.PagedMediaException
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcMetadata}
import com.xmlcalabash.util.{MediaType, S9Api}
import jp.co.antenna.XfoJavaCtl.{MessageListener, XfoFormatPageListener, XfoObj}
import net.sf.saxon.s9api.{QName, Serializer, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, OutputStream, PrintStream}
import java.net.URI
import scala.collection.mutable.ListBuffer

class CssAH extends CssProcessor {
  private val _OptionsFileURI = new QName("", "OptionsFileURI")
  private val _ExitLevel = new QName("", "ExitLevel")
  private val _EmbedAllFontsEx = new QName("", "EmbedAllFontsEx")
  private val _ImageCompression = new QName("", "ImageCompression")
  private val _NoAccessibility = new QName("", "NoAccessibility")
  private val _NoAddingOrChangingComments = new QName("", "NoAddingOrChangingComments")
  private val _NoAssembleDoc = new QName("", "NoAssembleDoc")
  private val _NoChanging = new QName("", "NoChanging")
  private val _NoContentCopying = new QName("", "NoContentCopying")
  private val _NoFillForm = new QName("", "NoFillForm")
  private val _NoPrinting = new QName("", "NoPrinting")
  private val _OwnersPassword = new QName("", "OwnersPassword")
  private val _TwoPassFormatting = new QName("", "TwoPassFormatting")

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var runtime: XMLCalabashRuntime = _
  private var options: Map[QName,XdmValue] = _

  private var ah: XfoObj = _
  private var primarySS = Option.empty[String]
  private val userSS = ListBuffer.empty[String]

  override def initialize(runtime: XMLCalabashRuntime, options: Map[QName, XdmValue]): Unit = {
    this.runtime = runtime
    this.options = options

    ah = new XfoObj()
    ah.setFormatterType(XfoObj.S_FORMATTERTYPE_XMLCSS)
    val msgs = new FoMessages()
    ah.setMessageListener(msgs)

    if (options.contains(_OptionsFileURI)) {
      ah.setOptionFileURI(options(_OptionsFileURI).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_ExitLevel)) {
      ah.setExitLevel(options(_ExitLevel).getUnderlyingValue.getStringValue.toInt)
    }

    if (options.contains(_EmbedAllFontsEx)) {
      val embed = options(_EmbedAllFontsEx).getUnderlyingValue.getStringValue
      embed match {
        case "part" => ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_PART)
        case "base14" => ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_BASE14)
        case "all" => ah.setPdfEmbedAllFontsEx(XfoObj.S_PDF_EMBALLFONT_ALL)
        case _ => logger.error(s"Ignoring unknown EmbedAllFontsEx option: ${embed}")
      }
    }

    if (options.contains(_ImageCompression)) {
      ah.setPdfImageCompression(options(_ImageCompression).getUnderlyingValue.getStringValue.toInt)
    }

    if (options.contains(_NoAccessibility)) {
      ah.setPdfNoAccessibility(options(_NoAccessibility).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_NoAddingOrChangingComments)) {
      ah.setPdfNoAddingOrChangingComments(options(_NoAddingOrChangingComments).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_NoAssembleDoc)) {
      ah.setPdfNoAssembleDoc(options(_NoAssembleDoc).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_NoChanging)) {
      ah.setPdfNoChanging(options(_NoChanging).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_NoContentCopying)) {
      ah.setPdfNoContentCopying(options(_NoContentCopying).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_NoFillForm)) {
      ah.setPdfNoFillForm(options(_NoFillForm).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_NoPrinting)) {
      ah.setPdfNoPrinting(options(_NoPrinting).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_OwnersPassword)) {
      ah.setPdfOwnerPassword(options(_OwnersPassword).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_TwoPassFormatting)) {
      ah.setTwoPassFormatting(options(_TwoPassFormatting).getUnderlyingValue.effectiveBooleanValue())
    }
  }

  override def addStylesheet(uri: String): Unit = {
    if (primarySS.isDefined) {
      userSS += uri
    } else {
      primarySS = Some(uri)
    }
  }

  override def addStylesheet(doc: XdmNode, meta: XProcMetadata): Unit = {
    if (meta.contentType.mediaType != "text") {
      logger.error(s"Ignoring non-text CSS stylesheet: ${meta.baseURI.getOrElse("unknown")}")
      return
    }

    val temp = File.createTempFile("xmlcalabash-focss", ".css")
    temp.deleteOnExit()

    val cssout = new PrintStream(temp)
    cssout.print(doc.getStringValue)
    cssout.close()

    addStylesheet(temp.toURI.toASCIIString)
  }

  override def format(doc: XdmNode, contentType: MediaType, out: OutputStream): Unit = {
    val outputFormat = if (FoAH.formatMap.contains(contentType)) {
      FoAH.formatMap(contentType)
    } else {
      throw PagedMediaException.xdUnsupportedContentType(s"Unsupported content type: ${contentType}")
    }

    if (primarySS.isEmpty) {
      logger.error("No CSS stylesheet provided for p:css-formatter")
    } else {
      ah.setStylesheetURI(primarySS.get)
    }

    for (css <- userSS) {
      ah.addUserStylesheetURI(css)
    }

    val serializer = runtime.processor.newSerializer()
    serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
    val baos = new ByteArrayOutputStream()
    serializer.setOutputStream(baos)
    S9Api.serialize(runtime.config, doc, serializer)

    val bis = new ByteArrayInputStream(baos.toByteArray)
    ah.render(bis, out, outputFormat)
    ah.releaseObjectEx();
  }

  private class FoMessages extends MessageListener with XfoFormatPageListener {
    override def onMessage(errLevel: Int, errCode: Int, errMessage: String): Unit = {
      errLevel match {
        case 1 =>
          logger.info(errMessage)
        case 2 =>
          logger.warn(errMessage)
        case _ =>
          logger.error(errMessage)
      }
    }

    override def onFormatPage(pageNo: Int): Unit = {
      logger.debug(s"Formatted PDF page ${pageNo}")
    }
  }
}
