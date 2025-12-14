package apps.emojionary

import munit.FunSuite
import cs214.webapp.*
import scala.util.{Try, Success, Failure}

class LogicTest extends FunSuite {

  private val sm = new Logic
  private val u0: String = "u0"
  private val u1: String = "u1"
  private val u2: String = "u2"

  private def renderState(res: Try[Seq[Action[State]]]): State =
    res match {
      case Failure(ex) => fail(s"Expected success but got failure: ${ex.getMessage}")
      case Success(actions) =>
        actions.collectFirst { case Action.Render(st) => st }
          .getOrElse(fail("Expected an Action.Render(...) in actions"))
    }

  private def failWith[T <: Throwable](res: Try[?], cls: Class[T]): Unit =
    res match {
      case Failure(ex) =>
        assert(cls.isInstance(ex), s"Expected ${cls.getName} but got ${ex.getClass.getName}")
      case Success(_) =>
        fail(s"Expected failure ${cls.getName} but got Success")
    }

  private def send(st: State, uid: UserId, e: Event): Try[Seq[Action[State]]] =
    sm.transition(st)(uid, Right(e))

  private def tick(st: State, millis: Long): Try[Seq[Action[State]]] =
    sm.transition(st)(u0, Left(Tick(millis)))

  private def init2(): State = sm.init(Seq(u0, u1))
  
  private def init3(): State = sm.init(Seq(u0, u1, u2))

  test("init: WaitingToStart, scores=0") {
    val st = init2()
    assertEquals(st.phase, Phase.WaitingToStart, "init phase")
    assertEquals(st.scores(u0), 0, "score u0")
    assertEquals(st.scores(u1), 0, "score u1")
    assertEquals(st.describerIndex, 0, "describerIndex should start at 0")
  }

  test("StartGame: WaitingToStart -> WaitingForHint") {
    val st0 = init2()
    val st1 = renderState(send(st0, u0, Event.StartGame))
    assertEquals(st1.phase, Phase.WaitingForHint, "phase after StartGame")
  }

  test("StartGame: fails if only 1 player") {
    val st1p = sm.init(Seq(u0))
    failWith(send(st1p, u0, Event.StartGame), classOf[IllegalStateException])
  }

  test("SendFirstHint: describer only, valid emoji, non-empty word") {
    val st0 = init2()
    val st1 = renderState(send(st0, u0, Event.StartGame))
    val emoji = sm.AllEmojis.head

    val st2 = renderState(send(st1, u0, Event.SendFirstHint("Scala", emoji)))
    assertEquals(st2.phase, Phase.Guessing, "phase should be Guessing")
    assertEquals(st2.secretWord, Some("scala"), "secretWord should be lowercased")
    assertEquals(st2.hints, List(emoji), "first hint stored")
    assert(st2.timerEndMs > 0L, "timerEndMs should be set")
  }

  test("SendFirstHint: fails if emoji invalid") {
    val st0 = init2()
    val st1 = renderState(send(st0, u0, Event.StartGame))
    failWith(send(st1, u0, Event.SendFirstHint("scala", "NOT_AN_EMOJI")), classOf[IllegalArgumentException])
  }

  test("SubmitGuess: wrong guess -> stays Guessing and appends guess") {
    // Need 3 players so that when u1 guesses wrong, u2 hasn't guessed yet
    // so everyoneAnswered = false and we stay in Guessing
    val st0 = init3()
    val st1 = renderState(send(st0, u0, Event.StartGame))
    val emoji = sm.AllEmojis.head
    val st2 = renderState(send(st1, u0, Event.SendFirstHint("scala", emoji)))

    // u1 guesses wrong - should stay in Guessing because u2 hasn't guessed yet
    val st3 = renderState(send(st2, u1, Event.SubmitGuess("python")))
    assertEquals(st3.phase, Phase.Guessing, "should remain Guessing")
    assert(st3.guesses.exists(_.text == "python"), "should contain guess")
    assertEquals(st3.winner, None, "no winner on wrong guess")
  }

  test("SubmitGuess: correct guess -> GameEnd + winner + points") {
    val st0 = init2()
    val st1 = renderState(send(st0, u0, Event.StartGame))
    val emoji = sm.AllEmojis.head
    val st2 = renderState(send(st1, u0, Event.SendFirstHint("scala", emoji))) // hintCount=1

    val st3 = renderState(send(st2, u1, Event.SubmitGuess("scala")))
    assertEquals(st3.phase, Phase.GameEnd, "should go to GameEnd")
    assertEquals(st3.winner, Some(u1), "winner should be guesser")
    assertEquals(st3.scores(u1), 5, "points should be MaxHints - hintCount + 1 = 5")
  }

  test("SubmitGuess: describer cannot guess") {
    val st0 = init2()
    val st1 = renderState(send(st0, u0, Event.StartGame))
    val emoji = sm.AllEmojis.head
    val st2 = renderState(send(st1, u0, Event.SendFirstHint("scala", emoji)))

    failWith(send(st2, u0, Event.SubmitGuess("scala")), classOf[IllegalStateException])
  }

  test("Tick: Guessing timer expired + max hints -> GameEnd (no winner)") {
    // Use 5 DIFFERENT emojis to satisfy State invariant (no duplicates)
    val fiveUniqueEmojis = sm.AllEmojis.take(Rules.MaxHints).toList
    val st = init2().copy(
      phase = Phase.Guessing,
      secretWord = Some("scala"),
      hints = fiveUniqueEmojis,
      timerEndMs = 0L,
      winner = None
    )
    val st2 = renderState(tick(st, 1L))
    assertEquals(st2.phase, Phase.GameEnd, "should go to GameEnd")
    assertEquals(st2.winner, None, "no winner when max hints reached")
  }

  test("Tick: GameEnd timer expired -> new game WaitingForHint and describer becomes winner") {
    val st = init2().copy(
      phase = Phase.GameEnd,
      winner = Some(u1),
      timerEndMs = 0L
    )
    val st2 = renderState(tick(st, 1L))
    assertEquals(st2.phase, Phase.WaitingForHint, "should start next round")
    assertEquals(st2.describerIndex, st2.playerOrder.indexOf(u1), "winner becomes describer")
    assertEquals(st2.secretWord, None, "secretWord reset")
    assertEquals(st2.hints, Nil, "hints reset")
    assertEquals(st2.guesses, Nil, "guesses reset")
    assertEquals(st2.winner, None, "winner reset")
  }
}