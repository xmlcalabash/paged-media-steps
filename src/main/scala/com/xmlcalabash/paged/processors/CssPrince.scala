package com.xmlcalabash.paged.processors

import com.princexml.{Prince, PrinceEvents}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.paged.config.CssProcessor
import com.xmlcalabash.paged.exceptions.PagedMediaException
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcMetadata}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{QName, Serializer, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import java.io._
import java.nio.file.Paths
import scala.collection.mutable.ListBuffer

class CssPrince extends CssProcessor {
  private val _exePath = new QName("", "exePath")
  private val _baseURL = new QName("", "baseURL")
  private val _compress = new QName("", "compress")
  private val _debug = new QName("", "debug")
  private val _embedFonts = new QName("", "embedFonts")
  private val _encrypt = new QName("", "encrypt")
  private val _keyBits = new QName("", "keyBits")
  private val _userPassword = new QName("", "userPassword")
  private val _ownerPassword = new QName("", "ownerPassword")
  private val _disallowPrint = new QName("", "disallowPrint")
  private val _disallowModify = new QName("", "disallowModify")
  private val _disallowCopy = new QName("", "disallowCopy")
  private val _disallowAnnotate = new QName("", "disallowAnnotate")
  private val _fileRoot = new QName("", "fileRoot")
  private val _html = new QName("", "html")
  private val _httpPassword = new QName("", "httpPassword")
  private val _httpUsername = new QName("", "httpUsername")
  private val _httpProxy = new QName("", "httpProxy")
  private val _inputType = new QName("", "inputType")
  private val _javascript = new QName("", "javascript")
  private val _log = new QName("", "log")
  private val _network = new QName("", "network")
  private val _subsetFonts = new QName("", "subsetFonts")
  private val _verbose = new QName("", "verbose")
  private val _XInclude = new QName("", "XInclude")
  private val _scripts = new QName("", "scripts")

  private val APPLICATION_PDF = new MediaType("application", "pdf")

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var runtime: XMLCalabashRuntime = _
  private var options: Map[QName,XdmValue] = _

  private var prince: Prince = _
  private var primarySS = Option.empty[String]
  private val userSS = ListBuffer.empty[String]
  private val tempFiles = ListBuffer.empty[File]

  override def initialize(runtime: XMLCalabashRuntime, options: Map[QName, XdmValue]): Unit = {
    this.runtime = runtime
    this.options = options
    primarySS = None
    userSS.clear()
    tempFiles.clear()

    val exePath = if (options.contains(_exePath)) {
      options(_exePath).getUnderlyingValue.getStringValue
    } else {
      if (Option(System.getProperty("com.xmlcalabash.css.prince.exepath")).isDefined) {
        System.getProperty("com.xmlcalabash.css.prince.exepath")
      } else {
        val exeName = if (System.getProperty("os.name").startsWith("Windows")) {
          "prince.exe"
        } else {
          "prince"
        }
        var found = ""
        for (path <- System.getenv("PATH").split(System.getProperty("path.separator"))) {
          val exe = Paths.get(path, exeName).toFile
          if (exe.exists() && exe.canExecute) {
            found = exe.getAbsolutePath
          }
        }
        found
      }
    }
    if (exePath == "") {
      throw XProcException.xdGeneralError("Attempted to use Prince, but executable cannot be found", None)
    }

    prince = new Prince(exePath, new PrinceMessages())

    if (options.contains(_baseURL)) {
      prince.setBaseURL(options(_baseURL).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_compress)) {
      prince.setCompress(options(_compress).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_debug)) {
      prince.setDebug(options(_debug).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_embedFonts)) {
      prince.setEmbedFonts(options(_embedFonts).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_encrypt)) {
      prince.setEncrypt(options(_encrypt).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_keyBits)) {
      val keyBits = options(_keyBits).getUnderlyingValue.getStringValue.toInt
      var up = ""
      var op = ""
      var dp = false
      var dm = false
      var dc = false
      var da = false

      if (options.contains(_userPassword)) {
        up = options(_userPassword).getUnderlyingValue.getStringValue
      }

      if (options.contains(_ownerPassword)) {
        op = options(_ownerPassword).getUnderlyingValue.getStringValue
      }

      if (options.contains(_disallowPrint)) {
        dp = options(_disallowPrint).getUnderlyingValue.effectiveBooleanValue()
      }

      if (options.contains(_disallowModify)) {
        dm = options(_disallowModify).getUnderlyingValue.effectiveBooleanValue()
      }

      if (options.contains(_disallowCopy)) {
        dc = options(_disallowCopy).getUnderlyingValue.effectiveBooleanValue()
      }

      if (options.contains(_disallowAnnotate)) {
        da = options(_disallowAnnotate).getUnderlyingValue.effectiveBooleanValue()
      }

      prince.setEncryptInfo(keyBits, up, op, dp, dm, dc, da)
    }

    if (options.contains(_fileRoot)) {
      prince.setFileRoot(options(_fileRoot).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_html)) {
      prince.setHTML(options(_html).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_httpPassword)) {
      prince.setHttpPassword(options(_httpPassword).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_httpUsername)) {
      prince.setHttpUsername(options(_httpUsername).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_httpProxy)) {
      prince.setHttpProxy(options(_httpProxy).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_inputType)) {
      prince.setInputType(options(_inputType).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_javascript)) {
      prince.setJavaScript(options(_javascript).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_log)) {
      prince.setLog(options(_log).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_network)) {
      prince.setNetwork(options(_network).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_subsetFonts)) {
      prince.setSubsetFonts(options(_subsetFonts).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_verbose)) {
      prince.setVerbose(options(_verbose).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_XInclude)) {
      prince.setXInclude(options(_XInclude).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_scripts)) {
      for (script <- options(_scripts).getUnderlyingValue.getStringValue.split("\\s+")) {
        prince.addScript(script)
      }
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

    val temp = File.createTempFile("xmlcalabash-princecss", ".css")
    temp.deleteOnExit()

    tempFiles += temp

    val cssout = new PrintStream(temp)
    cssout.print(doc.getStringValue)
    cssout.close()

    addStylesheet(temp.toURI.toASCIIString)
  }

  override def format(doc: XdmNode, contentType: MediaType, out: OutputStream): Unit = {
    if (contentType != APPLICATION_PDF) {
      throw PagedMediaException.xdUnsupportedContentType(s"Unsupported content type: ${contentType}")
    }

    if (primarySS.isDefined) {
      prince.addStyleSheet(primarySS.get)
    }

    for (css <- userSS) {
      prince.addStyleSheet(css)
    }

    val serializer = runtime.processor.newSerializer()
    serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
    val baos = new ByteArrayOutputStream()
    serializer.setOutputStream(baos)
    S9Api.serialize(runtime.config, doc, serializer)

    val bis = new ByteArrayInputStream(baos.toByteArray)
    prince.convert(bis, out)

    for (temp <- tempFiles) {
      try {
        temp.delete()
      } catch {
        case _: Throwable =>
          ()
      }
    }
  }

  private class PrinceMessages extends PrinceEvents {
    override def onMessage(msgType: String, msgLoc: String, message: String): Unit = {
      msgType match {
        case "inf" => logger.info(message)
        case "wrn" => logger.warn(message)
        case _ => logger.error(message)
      }
    }
  }
}
