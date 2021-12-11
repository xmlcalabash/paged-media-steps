package com.xmlcalabash.paged.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.paged.config.FoProcessor
import com.xmlcalabash.paged.exceptions.PagedMediaException
import com.xmlcalabash.runtime.{BinaryNode, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{MediaType, MinimalStaticContext, PipelineEnvironmentOptionString, URIUtils}
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

import java.io.ByteArrayOutputStream
import java.net.URI
import scala.collection.mutable.ListBuffer

class XslFormatter extends DefaultXmlStep {
  private val cc_fo_processor = "Q{http://xmlcalabash.com/ns/configuration}fo-processor"
  private var source: XdmNode = _
  private var sourceMeta: XProcMetadata = _
  private var parameters: Map[QName, XdmValue] = _
  private var contentType: MediaType = _
  private var baseURI: URI = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.MARKUPSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        source = node
        sourceMeta = metadata
        baseURI = node.getBaseURI
      case _ =>
        throw XProcException.xiThisCantHappen(s"Input on ${port} was not a node", location)
    }
  }

  override def run(context: MinimalStaticContext): Unit = {
    super.run(context)

    val ctString = optionalStringBinding(XProcConstants._content_type)
    if (ctString.isDefined) {
      contentType = MediaType.parse(ctString.get)
    } else {
      contentType = new MediaType("application", "pdf")
    }

    if (definedBinding(XProcConstants._parameters)) {
      parameters = ValueParser.parseParameters(mapBinding(XProcConstants._parameters), context)
    } else {
      parameters = Map()
    }

    val foClasses = ListBuffer.empty[String]
    for (opt <- config.config.parameters
      collect { case p: PipelineEnvironmentOptionString => p }
      filter { _.eqname == cc_fo_processor }) {
      foClasses += opt.value
    }
    foClasses += "com.xmlcalabash.paged.processors.FoFOP"

    var provider = Option.empty[FoProcessor]
    for (klass <- foClasses) {
      if (provider.isEmpty) {
        try {
          if (Option(baseURI).isEmpty || baseURI.toString == "") {
            baseURI = context.baseURI.getOrElse(URIUtils.cwdAsURI)
          }
          val thing = Class.forName(klass).getDeclaredConstructor().newInstance()
          provider = Some(thing.asInstanceOf[FoProcessor])
          provider.get.initialize(config, baseURI, parameters)
        } catch {
          case _: NoClassDefFoundError =>
            logger.debug(s"No FO processor class available: ${klass}")
          case ex: Throwable =>
            logger.debug(s"Failed to instantiate FO processor ${klass}: ${ex.getMessage}")
        }
      }
    }

    if (provider.isEmpty) {
      throw XProcException.xdGeneralError("Failed to instantiate XSL formatter", None)
    }

    val pdf = new ByteArrayOutputStream()
    try {
      provider.get.format(source, contentType, pdf)
    } catch {
      case ex: XProcException =>
        throw ex
      case ex: Throwable =>
        throw PagedMediaException.xdFormatterFailed(ex.getMessage, ex)
    }

    val node = new BinaryNode(config, pdf.toByteArray)
    if (sourceMeta.baseURI.isDefined) {
      consumer.receive("result", node, new XProcMetadata(contentType, sourceMeta.baseURI.get))
    } else {
      consumer.receive("result", node, new XProcMetadata(contentType))
    }
  }
}
