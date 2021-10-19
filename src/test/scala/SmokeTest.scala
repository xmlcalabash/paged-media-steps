import com.princexml.{Prince, PrinceEvents}
import com.renderx.xep.FormatterImpl
import com.xmlcalabash.exceptions.XProcException
import jp.co.antenna.XfoJavaCtl.XfoObj
import org.apache.fop.apps.FopFactoryBuilder
import org.scalatest.flatspec.AnyFlatSpec

import java.net.URI
import java.nio.file.Paths
import java.util.Properties

class SmokeTest extends AnyFlatSpec {

  "Instantiating AH " should " succeed" in {
    try {
      val ah = new XfoObj()
      assert(ah != null)
    } catch {
      case ex: Throwable =>
        System.err.println(ex.getMessage)
        ex.printStackTrace(System.err)
        fail()
    }
  }

  "Instantiating Prince " should " succeed" in {
    val exePath = if (Option(System.getProperty("com.xmlcalabash.css.prince.exepath")).isDefined) {
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
    if (exePath == "") {
      throw XProcException.xdGeneralError("Attempted to use Prince, but executable cannot be found", None)
    }

    val prince = new Prince(exePath, new PrinceMessages())
    assert(prince != null)
  }

  "Instantiating Xep " should " succeed" in {
    val props = new Properties()
    props.setProperty("CONFIG", "src/test/resources/xep.xml")
    val xep = new FormatterImpl(props, new FoLogger())
    assert(xep != null)
  }

  "Instantiating FOP " should " succeed" in {
    val fopFactoryBuilder = new FopFactoryBuilder(URI.create("file:///tmp/"))
    val userAgent = fopFactoryBuilder.build().newFOUserAgent()
    // Good enough
    assert(userAgent != null)
  }

  private class PrinceMessages extends PrinceEvents {
    override def onMessage(msgType: String, msgLoc: String, message: String): Unit = {
      msgType match {
        case "inf" => println(message)
        case "wrn" => println(message)
        case _ => println(message)
      }
    }
  }

  private class FoLogger extends com.renderx.xep.lib.Logger {
    override def openDocument(): Unit = {
      println("Xep processing begins")
    }

    override def closeDocument(): Unit = {
      println("Xep processing ends")
    }

    override def event(name: String, message: String): Unit = {
      println(s"Xep ${name}: ${message}")
    }

    override def openState(state: String): Unit = {
      println(s"Xep process starts: ${state}")
    }

    override def closeState(state: String): Unit = {
      println(s"Xep process ends: ${state}")
    }

    override def info(message: String): Unit = {
      println(message)
    }

    override def warning(message: String): Unit = {
      println(message)
    }

    override def error(message: String): Unit = {
      println(message)
    }

    override def exception(message: String, ex: Exception): Unit = {
      println(message)
      throw ex
    }
  }
}
