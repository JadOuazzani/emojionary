package apps.emojionary

import munit.FunSuite
import ujson.*

class WireTest extends FunSuite {

  test("Event wire: encode then decode returns same Event") {
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

  test("Event wire: unknown type fails") {
    val bad = Obj("type" -> "NotARealEvent")
    assert(Wire.eventFormat.decode(bad).isFailure, "decoding unknown event type should fail")
  }

  test("View wire: encode then decode returns same View") {
    val v = View(
      phase = Phase.Guessing,
      amIDescriber = false,
      allScores = Map("u0" -> 2, "u1" -> 5),
      hints = List("🔥", "💡"),
      guesses = List("scala", "java"),
      remainingTimeMs = 1234L,
      secretWord = Some("scala"),
      winner = None,
      availableEmojis = List("🔥", "💡", "❓")
    )

    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded, v, "round-trip failed for view")
  }

  test("View wire: Null secretWord/winner <-> None") {
    val v = View(
      phase = Phase.WaitingToStart,
      amIDescriber = true,
      allScores = Map.empty,
      hints = Nil,
      guesses = Nil,
      remainingTimeMs = 0L,
      secretWord = None,
      winner = None,
      availableEmojis = Nil
    )

    val json = Wire.viewFormat.encode(v)
    val decoded = Wire.viewFormat.decode(json).get
    assertEquals(decoded, v, "Null fields should decode back to None")
  }
}