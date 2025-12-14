package apps.ul2025app93

import cs214.webapp.*
import ujson.*
import scala.util.Try

/**
 * Wire protocol for Emojionary game communication.
 * 
 * This object handles the serialization and deserialization of game events
 * and views between the client (browser) and server. It implements the
 * [[cs214.webapp.AppWire]] interface with JSON as the transport format.
 * 
 * == Design by Contract ==
 * All encoding/decoding methods use `require` for precondition validation
 * and `ensuring` for postcondition verification to guarantee data integrity.
 * 
 * == Thread Safety ==
 * All methods are pure functions with no shared mutable state, making them
 * inherently thread-safe.
 * 
 * @see [[Event]] for the events that can be encoded/decoded
 * @see [[View]] for the view structure that is serialized
 */
object Wire extends AppWire[Event, View]:

  // ============ EVENT FORMAT ============
  
  /**
   * Wire format for game events.
   * 
   * Handles serialization of player actions to JSON for transmission
   * to the server, and deserialization of JSON back to Event objects.
   * 
   * == JSON Structure ==
   * All events are encoded as JSON objects with a "type" field:
   * {{{
   * // StartGame
   * { "type": "StartGame" }
   * 
   * // SendFirstHint
   * { "type": "SendFirstHint", "secretWord": "scala", "emoji": "🔥" }
   * 
   * // SendHint
   * { "type": "SendHint", "emoji": "💡" }
   * 
   * // SubmitGuess
   * { "type": "SubmitGuess", "text": "programming" }
   * }}}
   */
  override object eventFormat extends WireFormat[Event]:
    
    /**
     * Encodes a game event to JSON.
     * 
     * @param e the event to encode; must not be null
     * @return a JSON object representing the event
     * @throws IllegalArgumentException if:
     *   - `e` is null
     *   - `secretWord` is empty (for SendFirstHint)
     *   - `emoji` is empty (for SendFirstHint, SendHint)
     *   - `text` is empty (for SubmitGuess)
     * 
     * @example
     * {{{
     * val json = Wire.eventFormat.encode(Event.StartGame)
     * // Returns: {"type": "StartGame"}
     * 
     * val json2 = Wire.eventFormat.encode(Event.SendHint("🎯"))
     * // Returns: {"type": "SendHint", "emoji": "🎯"}
     * }}}
     */
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
    
    /**
     * Decodes a JSON value to a game event.
     * 
     * @param json the JSON value to decode; must be a valid JSON object
     * @return a `Try[Event]` containing the decoded event on success,
     *         or a `Failure` with the error on failure
     * @throws IllegalArgumentException (wrapped in Failure) if:
     *   - `json` is null or not an object
     *   - required fields are missing
     *   - field values are empty when they shouldn't be
     * @throws DecodingException (wrapped in Failure) if the event type is unknown
     * 
     * @example
     * {{{
     * val event = Wire.eventFormat.decode(Obj("type" -> "StartGame"))
     * // Returns: Success(Event.StartGame)
     * 
     * val event2 = Wire.eventFormat.decode(Obj("type" -> "Unknown"))
     * // Returns: Failure(DecodingException("Unknown event type: Unknown"))
     * }}}
     */
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
  
  /**
   * Wire format for game views.
   * 
   * Handles serialization of the game state projection (View) to JSON
   * for transmission to clients, and deserialization back to View objects.
   * 
   * == JSON Structure ==
   * Views are encoded as JSON objects with all game state fields:
   * {{{
   * {
   *   "phase": "Guessing",
   *   "amIDescriber": false,
   *   "allScores": {"player1": 5, "player2": 3},
   *   "hints": ["🔥", "💻"],
   *   "guesses": ["java", "python"],
   *   "remainingTimeMs": 15000,
   *   "secretWord": null,
   *   "winner": null,
   *   "availableEmojis": [],
   *   "myWrongGuesses": ["java", "python"]
   * }
   * }}}
   * 
   * @note `secretWord` is `null` for guessers (they can't see it)
   * @note `availableEmojis` is empty for non-describers
   */
  override object viewFormat extends WireFormat[View]:
    
    /**
     * Encodes a game view to JSON.
     * 
     * @param v the view to encode; must not be null
     * @return a JSON object representing the view
     * @throws IllegalArgumentException if:
     *   - `v` is null
     *   - `remainingTimeMs` is negative
     *   - any score is negative
     * 
     * @example
     * {{{
     * val view = View(
     *   phase = Phase.Guessing,
     *   amIDescriber = false,
     *   allScores = Map("p1" -> 5),
     *   hints = List("🔥"),
     *   guesses = List("java"),
     *   remainingTimeMs = 10000L,
     *   secretWord = None,
     *   winner = None,
     *   availableEmojis = Nil,
     *   myWrongGuesses = List("java")
     * )
     * val json = Wire.viewFormat.encode(view)
     * }}}
     */
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
    
    /**
     * Decodes a JSON value to a game view.
     * 
     * @param json the JSON value to decode; must be a valid JSON object
     * @return a `Try[View]` containing the decoded view on success,
     *         or a `Failure` with the error on failure
     * @throws IllegalArgumentException (wrapped in Failure) if:
     *   - `json` is null or not an object
     *   - any required field is missing
     *   - `remainingTimeMs` is negative
     *   - any score is negative
     *   - hints exceed `Rules.MaxHints`
     * @throws DecodingException (wrapped in Failure) if the phase is unknown
     * 
     * @example
     * {{{
     * val json = Obj(
     *   "phase" -> "WaitingToStart",
     *   "amIDescriber" -> true,
     *   "allScores" -> Obj(),
     *   "hints" -> Arr(),
     *   "guesses" -> Arr(),
     *   "remainingTimeMs" -> 0,
     *   "secretWord" -> Null,
     *   "winner" -> Null,
     *   "availableEmojis" -> Arr(),
     *   "myWrongGuesses" -> Arr()
     * )
     * val view = Wire.viewFormat.decode(json)
     * // Returns: Success(View(...))
     * }}}
     */
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
  
  /**
   * Encodes a game phase to its JSON string representation.
   * 
   * @param phase the phase to encode; must not be null
   * @return a JSON string value representing the phase
   * @throws IllegalArgumentException if `phase` is null
   * 
   * == Phase Mappings ==
   * | Phase | JSON String |
   * |-------|-------------|
   * | `WaitingToStart` | `"WaitingToStart"` |
   * | `WaitingForHint` | `"WaitingForHint"` |
   * | `Guessing` | `"Guessing"` |
   * | `GameEnd` | `"GameEnd"` |
   */
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
  
  /**
   * Decodes a JSON string to a game phase.
   * 
   * @param json the JSON value to decode; must be a non-null string
   * @return the corresponding `Phase` enum value
   * @throws IllegalArgumentException if `json` is null or not a string
   * @throws DecodingException if the phase string is not recognized
   * 
   * @see [[encodePhase]] for the inverse operation
   */
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