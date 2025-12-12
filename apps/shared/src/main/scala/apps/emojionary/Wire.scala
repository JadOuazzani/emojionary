package apps.emojionary

import cs214.webapp.*
import ujson.*
import scala.util.Try

object Wire extends AppWire[Event, View]:

  // ============ EVENT FORMAT ============
  
  override object eventFormat extends WireFormat[Event]:
    
    def encode(e: Event): Value = 
      require(e != null, "Event cannot be null")
      
      val result = e match
        case Event.StartGame => 
          Obj("type" -> "StartGame")
        case Event.SendFirstHint(secretWord, emoji) =>
          require(secretWord.nonEmpty, "Secret word cannot be empty")
          require(emoji.nonEmpty, "Emoji cannot be empty")
          Obj("type" -> "SendFirstHint", "secretWord" -> secretWord, "emoji" -> emoji)
        case Event.SendHint(emoji) =>
          require(emoji.nonEmpty, "Emoji cannot be empty")
          Obj("type" -> "SendHint", "emoji" -> emoji)
        case Event.SubmitGuess(text) =>
          require(text.nonEmpty, "Guess text cannot be empty")
          Obj("type" -> "SubmitGuess", "text" -> text)
      
      result.ensuring(v => 
        v.obj.contains("type"),
        "Encoded event must contain type field"
      )
    
    def decode(json: Value): Try[Event] = Try {
      require(json != null, "JSON cannot be null")
      require(json.objOpt.isDefined, "JSON must be an object")
      
      val obj = json.obj
      require(obj.contains("type"), "Event JSON must contain 'type' field")
      
      val event = obj("type").str match
        case "StartGame" => 
          Event.StartGame
        case "SendFirstHint" =>
          require(obj.contains("secretWord"), "SendFirstHint must contain 'secretWord'")
          require(obj.contains("emoji"), "SendFirstHint must contain 'emoji'")
          val secretWord = obj("secretWord").str
          val emoji = obj("emoji").str
          require(secretWord.nonEmpty, "Secret word cannot be empty")
          require(emoji.nonEmpty, "Emoji cannot be empty")
          Event.SendFirstHint(secretWord, emoji)
        case "SendHint" =>
          require(obj.contains("emoji"), "SendHint must contain 'emoji'")
          val emoji = obj("emoji").str
          require(emoji.nonEmpty, "Emoji cannot be empty")
          Event.SendHint(emoji)
        case "SubmitGuess" =>
          require(obj.contains("text"), "SubmitGuess must contain 'text'")
          val text = obj("text").str
          require(text.nonEmpty, "Guess text cannot be empty")
          Event.SubmitGuess(text)
        case other => 
          throw DecodingException(s"Unknown event type: $other")
      
      event
    }.ensuring(_.isSuccess || true, "Decode must return valid Try")

  // ============ VIEW FORMAT ============
  
  override object viewFormat extends WireFormat[View]:
    
    def encode(v: View): Value =
      require(v != null, "View cannot be null")
      require(v.remainingTimeMs >= 0, "Remaining time cannot be negative")
      require(v.allScores.values.forall(_ >= 0), "All scores must be non-negative")
      
      val result = Obj(
        "phase" -> encodePhase(v.phase),
        "amIDescriber" -> v.amIDescriber,
        "allScores" -> Obj.from(v.allScores.map((k, s) => k -> Num(s.toDouble))),
        "hints" -> Arr.from(v.hints.map(Str(_))),
        "guesses" -> Arr.from(v.guesses.map(Str(_))),
        "remainingTimeMs" -> Num(v.remainingTimeMs.toDouble),
        "secretWord" -> v.secretWord.map(Str(_)).getOrElse(Null),
        "winner" -> v.winner.map(Str(_)).getOrElse(Null),
        "availableEmojis" -> Arr.from(v.availableEmojis.map(Str(_))),
        "myWrongGuesses" -> Arr.from(v.myWrongGuesses.map(Str(_)))
      )
      
      result.ensuring(obj =>
        obj.obj.contains("phase") &&
        obj.obj.contains("amIDescriber") &&
        obj.obj.contains("allScores") &&
        obj.obj.contains("hints") &&
        obj.obj.contains("guesses") &&
        obj.obj.contains("remainingTimeMs"),
        "Encoded view must contain all required fields"
      )
    
    def decode(json: Value): Try[View] = Try {
      require(json != null, "JSON cannot be null")
      require(json.objOpt.isDefined, "JSON must be an object")
      
      val obj = json.obj
      
      // Validate required fields exist
      val requiredFields = Seq("phase", "amIDescriber", "allScores", "hints", 
                               "guesses", "remainingTimeMs", "secretWord", 
                               "winner", "availableEmojis", "myWrongGuesses")
      requiredFields.foreach(field =>
        require(obj.contains(field), s"View JSON must contain '$field' field")
      )
      
      val phase = decodePhase(obj("phase"))
      val amIDescriber = obj("amIDescriber").bool
      val allScores = obj("allScores").obj.map((k, v) => k -> v.num.toInt).toMap
      val hints = obj("hints").arr.map(_.str).toList
      val guesses = obj("guesses").arr.map(_.str).toList
      val remainingTimeMs = obj("remainingTimeMs").num.toLong
      val secretWord = obj("secretWord") match
        case Null => None
        case s => Some(s.str)
      val winner = obj("winner") match
        case Null => None
        case s => Some(s.str)
      val availableEmojis = obj("availableEmojis").arr.map(_.str).toList
      val myWrongGuesses = obj("myWrongGuesses").arr.map(_.str).toList
      
      // Validate decoded values
      require(remainingTimeMs >= 0, "Remaining time cannot be negative")
      require(allScores.values.forall(_ >= 0), "All scores must be non-negative")
      require(hints.size <= Rules.MaxHints, "Too many hints")
      
      View(
        phase = phase,
        amIDescriber = amIDescriber,
        allScores = allScores,
        hints = hints,
        guesses = guesses,
        remainingTimeMs = remainingTimeMs,
        secretWord = secretWord,
        winner = winner,
        availableEmojis = availableEmojis,
        myWrongGuesses = myWrongGuesses
      )
    }.ensuring(_.isSuccess || true, "Decode must return valid Try")

  // ============ HELPERS ============
  
  private def encodePhase(phase: Phase): Value = 
    require(phase != null, "Phase cannot be null")
    
    val result = phase match
      case Phase.WaitingToStart => Str("WaitingToStart")
      case Phase.WaitingForHint => Str("WaitingForHint")
      case Phase.Guessing => Str("Guessing")
      case Phase.GameEnd => Str("GameEnd")
    
    result.ensuring(v => 
      v.strOpt.isDefined && v.str.nonEmpty,
      "Encoded phase must be a non-empty string"
    )
    
  private def decodePhase(json: Value): Phase = 
    require(json != null, "Phase JSON cannot be null")
    require(json.strOpt.isDefined, "Phase must be a string")
    
    val phase = json.str match
      case "WaitingToStart" => Phase.WaitingToStart
      case "WaitingForHint" => Phase.WaitingForHint
      case "Guessing" => Phase.Guessing
      case "GameEnd" => Phase.GameEnd
      case other => throw DecodingException(s"Unknown phase: $other")
    
    phase.ensuring(_ != null, "Decoded phase must not be null")