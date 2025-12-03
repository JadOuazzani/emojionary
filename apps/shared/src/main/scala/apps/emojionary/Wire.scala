package apps.emojionary

import cs214.webapp.*
import ujson.*
import scala.util.Try

object Wire extends AppWire[Event, View]:

  // ============ EVENT FORMAT ============

  override object eventFormat extends WireFormat[Event]:
    
    def encode(e: Event): Value = e match
      case Event.StartGame => 
        Obj("type" -> "StartGame")
      case Event.SendFirstHint(secretWord, emoji) => 
        Obj("type" -> "SendFirstHint", "secretWord" -> secretWord, "emoji" -> emoji)
      case Event.SendHint(emoji) => 
        Obj("type" -> "SendHint", "emoji" -> emoji)
      case Event.SubmitGuess(text) => 
        Obj("type" -> "SubmitGuess", "text" -> text)
    
    def decode(json: Value): Try[Event] = Try {
      val obj = json.obj
      obj("type").str match
        case "StartGame" => Event.StartGame
        case "SendFirstHint" => Event.SendFirstHint(obj("secretWord").str, obj("emoji").str)
        case "SendHint" => Event.SendHint(obj("emoji").str)
        case "SubmitGuess" => Event.SubmitGuess(obj("text").str)
        case other => throw DecodingException(s"Unknown event type: $other")
    }

  // ============ VIEW FORMAT ============

  override object viewFormat extends WireFormat[View]:
    
    def encode(v: View): Value =
      Obj(
        "phase" -> encodePhase(v.phase),
        "amIDescriber" -> v.amIDescriber,
        "allScores" -> Obj.from(v.allScores.map((k, s) => k -> Num(s.toDouble))),
        "hints" -> Arr.from(v.hints.map(Str(_))),
        "guesses" -> Arr.from(v.guesses.map(Str(_))),
        "remainingTimeMs" -> Num(v.remainingTimeMs.toDouble),
        "secretWord" -> v.secretWord.map(Str(_)).getOrElse(Null),
        "winner" -> v.winner.map(Str(_)).getOrElse(Null),
        "availableEmojis" -> Arr.from(v.availableEmojis.map(Str(_)))
      )
    
    def decode(json: Value): Try[View] = Try {
      val obj = json.obj
      View(
        phase = decodePhase(obj("phase")),
        amIDescriber = obj("amIDescriber").bool,
        allScores = obj("allScores").obj.map((k, v) => k -> v.num.toInt).toMap,
        hints = obj("hints").arr.map(_.str).toList,
        guesses = obj("guesses").arr.map(_.str).toList,
        remainingTimeMs = obj("remainingTimeMs").num.toLong,
        secretWord = obj("secretWord") match
          case Null => None
          case s => Some(s.str),
        winner = obj("winner") match
          case Null => None
          case s => Some(s.str),
        availableEmojis = obj("availableEmojis").arr.map(_.str).toList
      )
    }

  // ============ HELPERS ============

  private def encodePhase(phase: Phase): Value = phase match
    case Phase.WaitingToStart => Str("WaitingToStart")
    case Phase.WaitingForHint => Str("WaitingForHint")
    case Phase.Guessing => Str("Guessing")
    case Phase.GameEnd => Str("GameEnd")

  private def decodePhase(json: Value): Phase = json.str match
    case "WaitingToStart" => Phase.WaitingToStart
    case "WaitingForHint" => Phase.WaitingForHint
    case "Guessing" => Phase.Guessing
    case "GameEnd" => Phase.GameEnd
    case other => throw DecodingException(s"Unknown phase: $other")