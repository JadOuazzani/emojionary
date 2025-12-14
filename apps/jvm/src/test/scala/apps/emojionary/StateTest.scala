package apps.ul2025app93

import munit.FunSuite

class StateTest extends FunSuite {
  private val u0: String = "u0"
  private val u1: String = "u1"
  private val u2: String = "u2"

  private def baseState(
      phase: Phase,
      describerIndex: Int = 0,
      hints: List[Emoji] = Nil,
      secretWord: Option[Word] = None
  ): State =
    State(
      playerOrder = Vector(u0, u1),
      describerIndex = describerIndex,
      scores = Map(u0 -> 0, u1 -> 0),
      phase = phase,
      secretWord = secretWord,
      hints = hints,
      guesses = Nil,
      currentHintGuessers = Set.empty,
      timerEndMs = 0L,
      winner = None
    )

  // ========== BASIC FUNCTIONALITY ==========

  test("describer / isDescriber / isGuesser") {
    val st = baseState(Phase.WaitingToStart, describerIndex = 0)
    assertEquals(st.describer, u0)
    assert(st.isDescriber(u0))
    assert(!st.isDescriber(u1))
    assert(st.isGuesser(u1))
    assert(!st.isGuesser(u0))
  }

  test("describer changes with describerIndex") {
    val st0 = baseState(Phase.WaitingToStart, describerIndex = 0)
    val st1 = baseState(Phase.WaitingToStart, describerIndex = 1)
    assertEquals(st0.describer, u0)
    assertEquals(st1.describer, u1)
  }

  test("canStart requires at least 2 players") {
    val one = baseState(Phase.WaitingToStart).copy(
      playerOrder = Vector(u0),
      scores = Map(u0 -> 0)
    )
    val two = baseState(Phase.WaitingToStart)
    assert(!one.canStart)
    assert(two.canStart)
  }

  test("canSendHint only in WaitingForHint and only describer") {
    val waiting = baseState(Phase.WaitingForHint, describerIndex = 0)
    assert(waiting.canSendHint(u0))
    assert(!waiting.canSendHint(u1))

    val guessing = baseState(Phase.Guessing, hints = List("😀"), secretWord = Some("test"))
    assert(!guessing.canSendHint(u0))
    assert(!guessing.canSendHint(u1))
  }

  test("canGuess only in Guessing and NOT describer") {
    val guessing = baseState(Phase.Guessing, describerIndex = 0, hints = List("😀"), secretWord = Some("test"))
    assert(!guessing.canGuess(u0))
    assert(guessing.canGuess(u1))

    val waiting = baseState(Phase.WaitingForHint)
    assert(!waiting.canGuess(u0))
    assert(!waiting.canGuess(u1))
  }

  test("hintCount returns hints.size") {
    val st0 = baseState(Phase.WaitingForHint, hints = Nil)
    val st3 = baseState(Phase.Guessing, hints = List("😀", "😁", "😂"), secretWord = Some("test"))
    assertEquals(st0.hintCount, 0)
    assertEquals(st3.hintCount, 3)
  }

  test("reachedMaxHints true when hints >= MaxHints") {
    val maxHints = List.tabulate(Rules.MaxHints)(i => s"emoji$i")
    val st = baseState(Phase.Guessing, hints = maxHints, secretWord = Some("test"))
    assert(st.reachedMaxHints)

    val belowMax = baseState(Phase.Guessing, hints = maxHints.take(Rules.MaxHints - 1), secretWord = Some("test"))
    assert(!belowMax.reachedMaxHints)
  }

  // ========== REQUIRE VALIDATIONS ==========

  test("require: playerOrder cannot be empty") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart).copy(
        playerOrder = Vector.empty,
        scores = Map.empty
      )
    }
  }

  test("require: playerOrder must not contain duplicates") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart).copy(
        playerOrder = Vector(u0, u0),
        scores = Map(u0 -> 0)
      )
    }
  }

  test("require: describerIndex must be in bounds (negative)") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart, describerIndex = -1)
    }
  }

  test("require: describerIndex must be in bounds (too large)") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart, describerIndex = 5)
    }
  }

  test("require: scores must contain exactly playerOrder players") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart).copy(scores = Map(u0 -> 0))
    }
  }

  test("require: scores must contain exactly playerOrder players (extra player)") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart).copy(scores = Map(u0 -> 0, u1 -> 0, u2 -> 0))
    }
  }

  test("require: all scores must be non-negative") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart).copy(scores = Map(u0 -> -1, u1 -> 0))
    }
  }

  test("require: hints count cannot exceed MaxHints") {
    val tooManyHints = List.tabulate(Rules.MaxHints + 1)(i => s"emoji$i")
    intercept[IllegalArgumentException] {
      baseState(Phase.Guessing, hints = tooManyHints, secretWord = Some("test"))
    }
  }

  test("require: hints must not contain duplicates") {
    intercept[IllegalArgumentException] {
      baseState(Phase.Guessing, hints = List("😀", "😀"), secretWord = Some("test"))
    }
  }

  test("require: secretWord must be trimmed") {
    intercept[IllegalArgumentException] {
      baseState(Phase.Guessing, hints = List("😀"), secretWord = Some(" test "))
    }
  }

  test("require: secretWord must be lowercase") {
    intercept[IllegalArgumentException] {
      baseState(Phase.Guessing, hints = List("😀"), secretWord = Some("Test"))
    }
  }

  test("require: secretWord cannot be empty string") {
    intercept[IllegalArgumentException] {
      baseState(Phase.Guessing, hints = List("😀"), secretWord = Some(""))
    }
  }

  test("require: currentHintGuessers must be subset of players") {
    intercept[IllegalArgumentException] {
      baseState(Phase.Guessing, hints = List("😀"), secretWord = Some("test")).copy(
        currentHintGuessers = Set("unknown_player")
      )
    }
  }

  test("require: timerEndMs must be non-negative") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart).copy(timerEndMs = -1L)
    }
  }

  test("require: winner must be a player if present") {
    intercept[IllegalArgumentException] {
      baseState(Phase.GameEnd).copy(winner = Some("unknown_player"))
    }
  }

  test("require: WaitingToStart phase must have no secretWord") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart, secretWord = Some("test"))
    }
  }

  test("require: WaitingToStart phase must have no hints") {
    intercept[IllegalArgumentException] {
      baseState(Phase.WaitingToStart, hints = List("😀"))
    }
  }

  test("require: Guessing phase must have secretWord") {
    intercept[IllegalArgumentException] {
      baseState(Phase.Guessing, hints = List("😀"), secretWord = None)
    }
  }

  test("require: Guessing phase must have hints") {
    intercept[IllegalArgumentException] {
      baseState(Phase.Guessing, hints = Nil, secretWord = Some("test"))
    }
  }

  // ========== VALID EDGE CASES ==========

  test("valid: single player state (cannot start but valid)") {
    val st = State(
      playerOrder = Vector(u0),
      describerIndex = 0,
      scores = Map(u0 -> 0),
      phase = Phase.WaitingToStart,
      secretWord = None,
      hints = Nil,
      guesses = Nil,
      currentHintGuessers = Set.empty,
      timerEndMs = 0L,
      winner = None
    )
    assert(!st.canStart)
  }

  test("valid: GameEnd phase with winner") {
    val st = baseState(Phase.GameEnd).copy(winner = Some(u0))
    assertEquals(st.winner, Some(u0))
  }

  test("valid: three players") {
    val st = State(
      playerOrder = Vector(u0, u1, u2),
      describerIndex = 2,
      scores = Map(u0 -> 5, u1 -> 3, u2 -> 7),
      phase = Phase.WaitingToStart,
      secretWord = None,
      hints = Nil,
      guesses = Nil,
      currentHintGuessers = Set.empty,
      timerEndMs = 0L,
      winner = None
    )
    assertEquals(st.describer, u2)
    assert(st.canStart)
  }

  test("valid: currentHintGuessers is valid subset") {
    val st = baseState(Phase.Guessing, hints = List("😀"), secretWord = Some("test")).copy(
      currentHintGuessers = Set(u1)
    )
    assertEquals(st.currentHintGuessers, Set(u1))
  }
}