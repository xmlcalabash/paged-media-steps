package com.xmlcalabash.paged.processors

import com.xmlcalabash.paged.config.FoProcessor
import com.xmlcalabash.paged.exceptions.PagedMediaException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}
import org.apache.fop.apps.{FopFactory, FopFactoryBuilder}
import org.apache.fop.configuration.DefaultConfigurationBuilder

import java.io.{File, OutputStream}
import java.net.URI
import java.text.DateFormat
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.{SAXResult, SAXSource}

class FoFOP extends FoProcessor {
  private val _UserConfig = new QName("", "UserConfig")
  private val _StrictFOValidation = new QName("", "StrictFOValidation")
  private val _BreakIndentInheritanceOnReferenceAreaBoundary = new QName("", "BreakIndentInheritanceOnReferenceAreaBoundary")
  private val _SourceResolution = new QName("", "SourceResolution")
  private val _Base14KerningEnabled = new QName("", "Base14KerningEnabled")
  private val _PageHeight = new QName("", "PageHeight")
  private val _PageWidth = new QName("", "PageWidth")
  private val _TargetResolution = new QName("", "TargetResolution")
  private val _StrictUserConfigValidation = new QName("", "StrictUserConfigValidation")
  private val _StrictValidation = new QName("", "StrictValidation")
  private val _UseCache = new QName("", "UseCache")

  private val _Accessibility = new QName("", "Accessibility")
  private val _Author = new QName("", "Author")
  private val _ConserveMemoryPolicy = new QName("", "ConserveMemoryPolicy")
  private val _CreationDate = new QName("", "CreationDate")
  private val _Creator = new QName("", "Creator")
  private val _Keywords = new QName("", "Keywords")
  private val _LocatorEnabled = new QName("", "LocatorEnabled")
  private val _Producer = new QName("", "Producer")
  private val _Subject = new QName("", "Subject")
  private val _Title = new QName("", "Title")

  private var runtime: XMLCalabashRuntime = _
  private var options: Map[QName,XdmValue] = _

  private var fopFactory: FopFactory = _

  override def initialize(runtime: XMLCalabashRuntime, baseURI: URI, options: Map[QName, XdmValue]): Unit = {
    this.runtime = runtime
    this.options = options

    // Only FOP 2.x is supported

    val fopBuilder = if (options.contains(_UserConfig)) {
      val opt = options(_UserConfig).toString
      val cfgBuilder = new DefaultConfigurationBuilder()
      val cfg = cfgBuilder.buildFromFile(new File(opt))
      new FopFactoryBuilder(baseURI).setConfiguration(cfg)
    } else {
      new FopFactoryBuilder(baseURI)
    }

    if (options.contains(_StrictFOValidation)) {
      fopBuilder.setStrictFOValidation(options(_StrictFOValidation).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_BreakIndentInheritanceOnReferenceAreaBoundary)) {
      fopBuilder.setBreakIndentInheritanceOnReferenceAreaBoundary(options(_BreakIndentInheritanceOnReferenceAreaBoundary).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_Base14KerningEnabled)) {
      fopBuilder.getFontManager.setBase14KerningEnabled(options(_Base14KerningEnabled).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_StrictUserConfigValidation)) {
      fopBuilder.setStrictUserConfigValidation(options(_StrictUserConfigValidation).getUnderlyingValue.effectiveBooleanValue())
    }

    // Backwards compatibility with StrictUserConfigValidation
    if (options.contains(_StrictValidation)) {
      fopBuilder.setStrictUserConfigValidation(options(_StrictValidation).getUnderlyingValue.effectiveBooleanValue())
    }

    if (options.contains(_UseCache)) {
      if (!options(_UseCache).getUnderlyingValue.effectiveBooleanValue()) {
        fopBuilder.getFontManager.disableFontCache()
      }
    }

    if (options.contains(_SourceResolution)) {
      fopBuilder.setSourceResolution(options(_SourceResolution).getUnderlyingValue.getStringValue.toFloat)
    }

    if (options.contains(_TargetResolution)) {
      fopBuilder.setTargetResolution(options(_TargetResolution).getUnderlyingValue.getStringValue.toFloat)
    }

    if (options.contains(_PageHeight)) {
      fopBuilder.setPageHeight(options(_PageHeight).getUnderlyingValue.getStringValue)
    }
    if (options.contains(_PageWidth)) {
      fopBuilder.setPageWidth(options(_PageWidth).getUnderlyingValue.getStringValue)
    }

    fopFactory = fopBuilder.build()
  }

  override def format(doc: XdmNode, contentType: MediaType, out: OutputStream): Unit = {
    val outputFormat = contentType.toString()
    if (!List("application/pdf", "application/postscript", "application/x-afp",
             "application/rtf", "text/plain").contains(outputFormat)) {
      throw PagedMediaException.xdUnsupportedContentType(s"Unsupported content type: ${contentType}")
    }

    val fodoc = S9Api.xdmToInputSource(runtime.config, doc)
    val source = new SAXSource(fodoc)

    val userAgent = fopFactory.newFOUserAgent()

    if (options.contains(_Accessibility)) {
      userAgent.setAccessibility(options(_Accessibility).getUnderlyingValue.effectiveBooleanValue)
    }

    if (options.contains(_Author)) {
      userAgent.setAuthor(options(_Author).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_ConserveMemoryPolicy)) {
      userAgent.setConserveMemoryPolicy(options(_ConserveMemoryPolicy).getUnderlyingValue.effectiveBooleanValue)
    }

    if (options.contains(_CreationDate)) {
      val df = DateFormat.getDateInstance()
      val d = df.parse(options(_CreationDate).getUnderlyingValue.getStringValue)
      userAgent.setCreationDate(d)
    }

    if (options.contains(_Creator)) {
      userAgent.setCreator(options(_Creator).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_Keywords)) {
      userAgent.setKeywords(options(_Keywords).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_LocatorEnabled)) {
      userAgent.setLocatorEnabled(options(_LocatorEnabled).getUnderlyingValue.effectiveBooleanValue)
    }

    if (options.contains(_Producer)) {
      userAgent.setProducer(options(_Producer).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_Subject)) {
      userAgent.setSubject(options(_Subject).getUnderlyingValue.getStringValue)
    }

    if (options.contains(_TargetResolution)) {
      userAgent.setTargetResolution(options(_TargetResolution).getUnderlyingValue.getStringValue.toFloat)
    }

    if (options.contains(_Title)) {
      userAgent.setTitle(options(_Title).getUnderlyingValue.getStringValue)
    }

    val fop = userAgent.newFop(outputFormat, out)
    val defHandler = fop.getDefaultHandler

    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    transformer.transform(source, new SAXResult(defHandler))
  }
}
