package com.xmlcalabash.paged.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.paged.config.CssProcessor
import com.xmlcalabash.paged.exceptions.PagedMediaException
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

import java.io.ByteArrayOutputStream
import scala.collection.mutable.ListBuffer

class CssFormatter extends DefaultXmlStep {
  private val cc_css_processor = new QName("cc", XProcConstants.ns_cc, "css-processor")
  private var source: XdmNode = _
  private var sourceMeta: XProcMetadata = _
  private val stylesheets = ListBuffer.empty[Tuple2[XdmNode,XProcMetadata]]
  private var parameters: Map[QName, XdmValue] = _
  private var contentType: MediaType = _

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE, "stylesheet" -> PortCardinality.ZERO_OR_MORE),
    Map("source" -> (MediaType.MATCH_XML.map(_.toString) ++ MediaType.MATCH_HTML.map(_.toString)).toList,
      "stylesheet" -> List("text/css")))
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        if (port == "source") {
          source = node
          sourceMeta = metadata
        } else {
          stylesheets += Tuple2(node,metadata)
        }
      case _ =>
        throw XProcException.xiThisCantHappen(s"Input on ${port} was not a node", location)
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)
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

    val cssClasses = ListBuffer.empty[String]
    if (config.config.configSettings.get(cc_css_processor).isDefined) {
      cssClasses += config.config.configSettings.get(cc_css_processor).get.asString
    }

    var provider = Option.empty[CssProcessor]
    for (klass <- cssClasses) {
      if (provider.isEmpty) {
        try {
          val thing = Class.forName(klass).getDeclaredConstructor().newInstance()
          provider = Some(thing.asInstanceOf[CssProcessor])
          provider.get.initialize(config, parameters)
        } catch {
          case ex: NoClassDefFoundError =>
            logger.debug(s"No CSS processor class available: ${klass}")
          case ex: Throwable =>
            logger.debug(s"Failed to instantiate CSS processor ${klass}: ${ex.getMessage}")
        }
      }
    }

    if (provider.isEmpty) {
      throw XProcException.xdGeneralError("Failed to instantiate CSS formatter", None)
    }

    for (css <- stylesheets) {
      provider.get.addStylesheet(css._1, css._2)
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
