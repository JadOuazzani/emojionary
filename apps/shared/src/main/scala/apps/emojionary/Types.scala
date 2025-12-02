package apps.emojionary

import cs214.webapp.UserId

// Type aliases
type Emoji = String
type Word = String

// Constants
object Rules:
  val MaxHints: Int = 5
  val HintDurationMs: Long = 30_000
  val GameEndDurationMs: Long = 5_000

// Phases
enum Phase:
  case WaitingToStart
  case WaitingForHint
  case Guessing
  case GameEnd

// Events
enum Event:
  case StartGame
  case SendFirstHint(secretWord: Word, emoji: Emoji)
  case SendHint(emoji: Emoji)
  case SubmitGuess(text: String)

// State
case class State(
  playerOrder: Vector[UserId],
  describerIndex: Int,
  scores: Map[UserId, Int],
  phase: Phase,
  secretWord: Option[Word],
  hints: List[Emoji],
  guesses: List[String],
  timerEndMs: Long,
  winner: Option[UserId]
):
  def describer: UserId = playerOrder(describerIndex)
  
  def isDescriber(uid: UserId): Boolean = uid == describer
  
  def isGuesser(uid: UserId): Boolean = !isDescriber(uid)
  
  def canStart: Boolean = playerOrder.size >= 2
  
  def canSendHint(uid: UserId): Boolean =
    phase == Phase.WaitingForHint && isDescriber(uid)
  
  def canGuess(uid: UserId): Boolean =
    phase == Phase.Guessing && isGuesser(uid)
  
  def hintCount: Int = hints.size
  
  def reachedMaxHints: Boolean = hintCount >= Rules.MaxHints

// View
case class View(
  phase: Phase,
  amIDescriber: Boolean,
  allScores: Map[UserId, Int],
  hints: List[Emoji],
  guesses: List[String],
  remainingTimeMs: Long,
  secretWord: Option[Word],
  winner: Option[UserId],
  availableEmojis: List[Emoji]
)