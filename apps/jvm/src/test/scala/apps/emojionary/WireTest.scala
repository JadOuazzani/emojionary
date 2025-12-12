package apps.emojionary

import munit.FunSuite
import ujson.*

class WireTest extends FunSuite {

  // ========== EVENT ENCODING/DECODING ==========

  test("Event: StartGame round-trip") {
    val e = Event.StartGame
    val json = Wire.eventFormat.encode(e)
    val decoded = Wire.eventFormat.decode(json).get
    assertEquals(decoded, e)
  }

  test("Event: SendFirstHint round-trip") {
    val e = Event.SendFirstHint("scala", "🔥")
    val json = Wire.eventFormat.encode(e)
    val decoded = Wire.eventFormat.decode(json).get
    assertEquals(decoded, e)
  }

  test("Event: SendHint round-trip") {
    val e = Event.SendHint("💡")
    val json = Wire.eventFormat.encode(e)
    val decoded = Wire.eventFormat.decode(json).get
    assertEquals(decoded, e)
  }

  test("Event: SubmitGuess round-trip") {
    val e = Event.SubmitGuess("compiler")
    val json = Wire.eventFormat.encode(e)
    val decoded = Wire.eventFormat.decode(json).get
    assertEquals(decoded, e)
  }

  test("Event: all events round-trip") {
    val events = List(
      Event.StartGame,
      Event.SendFirstHint("scala", "🔥"),
      Event.SendHint("💡"),
      Event.SubmitGuess("compiler")
    )
    events.foreach { e =>
      val json = Wire.eventFormat.encode(e)
      val decoded = Wire.eventFormat.decode(json).get
      assertEquals(decoded, e, s"round-trip failed for event: $e")
    }
  }

  test("Event: unknown type fails") {
    val bad = Obj("type" -> "NotARealEvent")
    assert(Wire.eventFormat.decode(bad).isFailure)
  }

  test("Event: missing type field fails") {
    val bad = Obj("secretWord" -> "test")
    assert(Wire.eventFormat.decode(bad).isFailure)
  }

  test("Event: SendFirstHint missing secretWord fails") {
    val bad = Obj("type" -> "SendFirstHint", "emoji" -> "🔥")
    assert(Wire.eventFormat.decode(bad).isFailure)
  }

  test("Event: SendFirstHint missing emoji fails") {
    val bad = Obj("type" -> "SendFirstHint", "secretWord" -> "test")
    assert(Wire.eventFormat.decode(bad).isFailure)
  }

  test("Event: SendHint missing emoji fails") {
    val bad = Obj("type" -> "SendHint")
    assert(Wire.eventFormat.decode(bad).isFailure)
  }

  test("Event: SubmitGuess missing text fails") {
    val bad = Obj("type" -> "SubmitGuess")
    assert(Wire.eventFormat.decode(bad).isFailure)
  }

  test("Event: encode produces correct JSON structure") {
    val e = Event.SendFirstHint("hello", "👋")
    val json = Wire.eventFormat.encode(e)
    assertEquals(json("type").str, "SendFirstHint")
    assertEquals(json("secretWord").str, "hello")
    assertEquals(json("emoji").str, "👋")
  }

  // ========== VIEW ENCODING/DECODING ==========

  test("View: WaitingToStart round-trip") {
    val v = View(
      phase = Phase.WaitingToStart,
      amIDescriber = true,
      allScores = Map("u0" -> 0, "u1" -> 0),
      hints = Nil,
      guesses = Nil,
      remainingTimeMs = 0L,
      secretWord = None,
      winner = None,
      availableEmojis = Nil,
      myWrongGuesses = Nil
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded, v)
  }

  test("View: WaitingForHint as describer round-trip") {
    val v = View(
      phase = Phase.WaitingForHint,
      amIDescriber = true,
      allScores = Map("u0" -> 2, "u1" -> 5),
      hints = Nil,
      guesses = Nil,
      remainingTimeMs = 30000L,
      secretWord = Some("scala"),
      winner = None,
      availableEmojis = List("🔥", "💡", "❓"),
      myWrongGuesses = Nil
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded, v)
  }

  test("View: Guessing as guesser round-trip (no secretWord)") {
    val v = View(
      phase = Phase.Guessing,
      amIDescriber = false,
      allScores = Map("u0" -> 2, "u1" -> 5),
      hints = List("🔥", "💡"),
      guesses = List("java", "python"),
      remainingTimeMs = 15000L,
      secretWord = None,  // guesser cannot see secretWord
      winner = None,
      availableEmojis = Nil,  // guesser cannot see availableEmojis
      myWrongGuesses = List("java", "python")
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded, v)
  }

  test("View: Guessing as describer round-trip") {
    val v = View(
      phase = Phase.Guessing,
      amIDescriber = true,
      allScores = Map("u0" -> 2, "u1" -> 5),
      hints = List("🔥", "💡"),
      guesses = List("java", "python"),
      remainingTimeMs = 15000L,
      secretWord = Some("scala"),  // describer sees secretWord
      winner = None,
      availableEmojis = Nil,  // not in Guessing phase
      myWrongGuesses = Nil
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded, v)
  }

  test("View: GameEnd with winner round-trip") {
    val v = View(
      phase = Phase.GameEnd,
      amIDescriber = false,
      allScores = Map("u0" -> 10, "u1" -> 15),
      hints = List("🔥", "💡", "🎯"),
      guesses = List("scala"),
      remainingTimeMs = 0L,
      secretWord = Some("scala"),  // everyone sees at GameEnd
      winner = Some("u1"),
      availableEmojis = Nil,
      myWrongGuesses = List("java")
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded, v)
  }

  test("View: Null secretWord/winner encode to JSON Null") {
    val v = View(
      phase = Phase.WaitingToStart,
      amIDescriber = true,
      allScores = Map.empty,
      hints = Nil,
      guesses = Nil,
      remainingTimeMs = 0L,
      secretWord = None,
      winner = None,
      availableEmojis = Nil,
      myWrongGuesses = Nil
    )
    val json = Wire.viewFormat.encode(v)
    assertEquals(json("secretWord"), Null)
    assertEquals(json("winner"), Null)
  }

  test("View: JSON Null fields decode to None") {
    val json = Obj(
      "phase" -> "WaitingToStart",
      "amIDescriber" -> true,
      "allScores" -> Obj(),
      "hints" -> Arr(),
      "guesses" -> Arr(),
      "remainingTimeMs" -> 0,
      "secretWord" -> Null,
      "winner" -> Null,
      "availableEmojis" -> Arr(),
      "myWrongGuesses" -> Arr()
    )
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded.secretWord, None)
    assertEquals(decoded.winner, None)
  }

  test("View: empty allScores round-trip") {
    val v = View(
      phase = Phase.WaitingToStart,
      amIDescriber = false,
      allScores = Map.empty,
      hints = Nil,
      guesses = Nil,
      remainingTimeMs = 0L,
      secretWord = None,
      winner = None,
      availableEmojis = Nil,
      myWrongGuesses = Nil
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded.allScores, Map.empty)
  }

  test("View: large scores preserved") {
    val v = View(
      phase = Phase.GameEnd,
      amIDescriber = true,
      allScores = Map("u0" -> 999, "u1" -> 1000000),
      hints = Nil,
      guesses = Nil,
      remainingTimeMs = 0L,
      secretWord = None,
      winner = Some("u1"),
      availableEmojis = Nil,
      myWrongGuesses = Nil
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded.allScores("u0"), 999)
    assertEquals(decoded.allScores("u1"), 1000000)
  }

  test("View: missing field fails") {
    val json = Obj(
      "phase" -> "WaitingToStart",
      "amIDescriber" -> true
      // missing other fields
    )
    assert(Wire.viewFormat.decode(json).isFailure)
  }

  test("View: unknown phase fails") {
    val json = Obj(
      "phase" -> "UnknownPhase",
      "amIDescriber" -> true,
      "allScores" -> Obj(),
      "hints" -> Arr(),
      "guesses" -> Arr(),
      "remainingTimeMs" -> 0,
      "secretWord" -> Null,
      "winner" -> Null,
      "availableEmojis" -> Arr(),
      "myWrongGuesses" -> Arr()
    )
    assert(Wire.viewFormat.decode(json).isFailure)
  }

  // ========== PHASE ENCODING/DECODING ==========

  test("Phase: all phases round-trip through View") {
    val phases = List(
      Phase.WaitingToStart,
      Phase.WaitingForHint,
      Phase.Guessing,
      Phase.GameEnd
    )
    phases.foreach { phase =>
      val v = View(
        phase = phase,
        amIDescriber = true,
        allScores = Map.empty,
        hints = if phase == Phase.Guessing then List("😀") else Nil,
        guesses = Nil,
        remainingTimeMs = 0L,
        secretWord = if phase == Phase.Guessing || phase == Phase.WaitingForHint then Some("test") else None,
        winner = None,
        availableEmojis = if phase == Phase.WaitingForHint then List("😀", "😁") else Nil,
        myWrongGuesses = Nil
      )
      val json = Wire.viewFormat.encode(v)
      val decoded = Wire.viewFormat.decode(json).get
      assertEquals(decoded.phase, phase, s"phase round-trip failed for: $phase")
    }
  }

  // ========== UNICODE/EMOJI HANDLING ==========

  test("Event: emoji unicode preserved") {
    val emojis = List("🔥", "💡", "🎯", "🚀", "👋", "❤️", "🇫🇷")
    emojis.foreach { emoji =>
      val e = Event.SendHint(emoji)
      val json = Wire.eventFormat.encode(e)
      val decoded = Wire.eventFormat.decode(json).get
      assertEquals(decoded, e, s"emoji round-trip failed for: $emoji")
    }
  }

  test("View: emoji list preserved") {
    val v = View(
      phase = Phase.WaitingForHint,
      amIDescriber = true,
      allScores = Map.empty,
      hints = Nil,
      guesses = Nil,
      remainingTimeMs = 0L,
      secretWord = Some("test"),
      winner = None,
      availableEmojis = List("🔥", "💡", "🎯", "🚀", "👋"),
      myWrongGuesses = Nil
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded.availableEmojis, v.availableEmojis)
  }

  // ========== EDGE CASES ==========

  test("Event: empty string in SubmitGuess") {
    val e = Event.SubmitGuess("")
    val json = Wire.eventFormat.encode(e)
    val decoded = Wire.eventFormat.decode(json).get
    assertEquals(decoded, e)
  }

  test("Event: special characters in secretWord") {
    val e = Event.SendFirstHint("café", "☕")
    val json = Wire.eventFormat.encode(e)
    val decoded = Wire.eventFormat.decode(json).get
    assertEquals(decoded, e)
  }

  test("View: many hints preserved") {
    val v = View(
      phase = Phase.Guessing,
      amIDescriber = true,
      allScores = Map("u0" -> 0),
      hints = List("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣"),
      guesses = Nil,
      remainingTimeMs = 1000L,
      secretWord = Some("test"),
      winner = None,
      availableEmojis = Nil,
      myWrongGuesses = Nil
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded.hints.size, 5)
    assertEquals(decoded.hints, v.hints)
  }

  test("View: myWrongGuesses preserved") {
    val wrongGuesses = List("wrong1", "wrong2", "wrong3")
    val v = View(
      phase = Phase.Guessing,
      amIDescriber = false,
      allScores = Map("u0" -> 0),
      hints = List("😀"),
      guesses = wrongGuesses,
      remainingTimeMs = 1000L,
      secretWord = None,
      winner = None,
      availableEmojis = Nil,
      myWrongGuesses = wrongGuesses
    )
    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded.myWrongGuesses, wrongGuesses)
  }
}