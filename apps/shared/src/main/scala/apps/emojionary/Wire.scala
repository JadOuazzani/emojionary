package apps.emojionary

import cs214.webapp.*
import ujson.*
import scala.util.Try

object Wire extends AppWire[Event, View]:

  override object eventFormat extends WireFormat[Event]:
    def encode(e: Event): Value = e match
      case Event.Dummy => Str("Dummy")

    def decode(json: Value): Try[Event] = Try {
      json.str match
        case "Dummy" => Event.Dummy
        case other   => throw DecodingException(s"Unknown event: $other")
    }

  override object viewFormat extends WireFormat[View]:
    def encode(v: View): Value =
      Obj("message" -> Str(v.message))

    def decode(json: Value): Try[View] = Try {
      val obj = json.obj
      View(obj("message").str)
    }
