package apps.emojionary

import munit.FunSuite

class StateTest extends FunSuite {

  private val u0: String = "u0"
  private val u1: String = "u1"

  private def baseState(
      phase: Phase,
      describerIndex: Int = 0,
      hints: List[Emoji] = Nil
  ): State =
    State(
      playerOrder = Vector(u0, u1),
      describerIndex = describerIndex,
      scores = Map(u0 -> 0, u1 -> 0),
      phase = phase,
      secretWord = None,
      hints = hints,
      guesses = Nil,
      timerEndMs = 0L,
      winner = None
    )

  test("describer / isDescriber / isGuesser") {
    val st = baseState(Phase.WaitingToStart, describerIndex = 0)
    assertEquals(st.describer, u0, "describer should be playerOrder(describerIndex)")
    assert(st.isDescriber(u0), "u0 should be describer")
    assert(!st.isDescriber(u1), "u1 should not be describer")
    assert(st.isGuesser(u1), "u1 should be guesser")
    assert(!st.isGuesser(u0), "u0 should not be guesser")
  }

  test("canStart requires at least 2 players") {
    val one = baseState(Phase.WaitingToStart).copy(playerOrder = Vector(u0))
    val two = baseState(Phase.WaitingToStart).copy(playerOrder = Vector(u0, u1))
    assert(!one.canStart, "should not be able to start with 1 player")
    assert(two.canStart, "should be able to start with 2 players")
  }

  test("canSendHint only in WaitingForHint and describer") {
    val st = baseState(Phase.WaitingForHint, describerIndex = 0)
    assert(st.canSendHint(u0), "describer can send hint when WaitingForHint")
    assert(!st.canSendHint(u1), "guesser cannot send hint")
  }

  test("canGuess only in Guessing and NOT describer") {
    val st = baseState(Phase.Guessing, describerIndex = 0)
    assert(!st.canGuess(u0), "describer cannot guess")
    assert(st.canGuess(u1), "guesser can guess")
  }

  test("reachedMaxHints true when hints >= MaxHints") {
    val st = baseState(Phase.Guessing, hints = List.fill(Rules.MaxHints)("😀"))
    assert(st.reachedMaxHints, "should reach max hints when hints.size >= MaxHints")
  }
}