package apps.emojionary

import cs214.webapp.*
import ujson.*
import scala.util.Try

object Wire extends AppWire[Event, View]:

  override object eventFormat extends WireFormat[Event]:
    
    def encode(e: Event): Value = ???
    
    def decode(json: Value): Try[Event] = ???

  override object viewFormat extends WireFormat[View]:

    def encode(v: View): Value = ???
    
    def decode(json: Value): Try[View] = ???