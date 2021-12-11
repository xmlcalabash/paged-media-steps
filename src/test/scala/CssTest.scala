import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.messages.AnyItemMessage
import com.xmlcalabash.util.{MediaType, PipelineOutputConsumer}
import org.scalatest.flatspec.AnyFlatSpec

class CssTest extends AnyFlatSpec {
  val PDF = MediaType.parse("application/pdf")

  "Running css.xpl " should " produce a PDF" in {
    // Frustratingly, these don't run in CI.
    if (sys.env.contains("CIRCLECI")) {
      System.err.println("Skipping CSS test on CI host")
    } else {
      val calabash = XMLCalabash.newInstance()
      val pdf = new Consumer()
      val result = new PipelineOutputConsumer("result", pdf)
      calabash.args.parse(List("src/test/resources/css.xpl"))
      calabash.parameter(result)
      calabash.configure()
      calabash.run()
      if (pdf.message.isDefined) {
        pdf.message.get match {
          case msg: AnyItemMessage =>
            assert(msg.metadata.contentType == PDF)
          case _ => fail()
        }
      } else {
        fail()
      }
    }
  }

  private class Consumer extends DataConsumer {
    var _message = Option.empty[Message]

    def message: Option[Message] = _message

    override def consume(port: String, message: Message): Unit = {
      _message = Some(message)
    }
  }
}