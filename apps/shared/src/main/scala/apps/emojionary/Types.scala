package apps.emojionary

import cs214.webapp.UserId

// Type aliases
type Emoji = String
type Word = String

case class GuessEntry(
  player: UserId,
  text: String,
  correct: Boolean
):
  require(player.nonEmpty, "Player ID cannot be empty")
  require(text.trim.nonEmpty, "Guess text cannot be empty")

// Constants
object Rules:
  val MaxHints: Int = 5
  val HintDurationMs: Long = 25_000
  val GameEndDurationMs: Long = 20_000

  // Validate constants at load time
  require(MaxHints > 0, "MaxHints must be positive")
  require(HintDurationMs > 0, "HintDurationMs must be positive")
  require(GameEndDurationMs > 0, "GameEndDurationMs must be positive")

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
  guesses: List[GuessEntry],
  currentHintGuessers: Set[UserId],
  timerEndMs: Long,
  winner: Option[UserId]
):
  // Invariants
  require(playerOrder.nonEmpty, "Player order cannot be empty")
  require(playerOrder.distinct == playerOrder, "Player order must not contain duplicates")
  require(describerIndex >= 0 && describerIndex < playerOrder.size, 
    s"Describer index $describerIndex out of bounds [0, ${playerOrder.size})")
  require(scores.keySet == playerOrder.toSet, 
    "Scores must contain exactly the players in playerOrder")
  require(scores.values.forall(_ >= 0), "All scores must be non-negative")
  require(hints.size <= Rules.MaxHints, 
    s"Hints count ${hints.size} exceeds maximum ${Rules.MaxHints}")
  require(hints.distinct == hints, "Hints must not contain duplicate emojis")
  require(secretWord.forall(w => w == w.trim.toLowerCase), 
    "Secret word must be trimmed and lowercase")
  require(secretWord.forall(_.nonEmpty), "Secret word cannot be empty if present")
  require(currentHintGuessers.subsetOf(playerOrder.toSet), 
    "Current hint guessers must be players")
  require(timerEndMs >= 0, "Timer end must be non-negative")
  require(winner.forall(playerOrder.contains), "Winner must be a player")
  
  // Phase-specific invariants
  require(
    phase match
      case Phase.WaitingToStart => secretWord.isEmpty && hints.isEmpty
      case Phase.WaitingForHint => winner.isEmpty
      case Phase.Guessing => secretWord.isDefined && hints.nonEmpty
      case Phase.GameEnd => true
    , s"Phase $phase has invalid state configuration"
  )

  def describer: UserId = 
    playerOrder(describerIndex)
      .ensuring(playerOrder.contains(_), "Describer must be in player order")
  
  def isDescriber(uid: UserId): Boolean = 
    uid == describer
  
  def isGuesser(uid: UserId): Boolean = 
    !isDescriber(uid)
  
  def canStart: Boolean = 
    playerOrder.size >= 2
  
  def canSendHint(uid: UserId): Boolean =
    (phase == Phase.WaitingForHint && isDescriber(uid))
      .ensuring(result => !result || playerOrder.contains(uid), 
        "Only valid players can send hints")
  
  def canGuess(uid: UserId): Boolean =
    (phase == Phase.Guessing && isGuesser(uid))
      .ensuring(result => !result || playerOrder.contains(uid),
        "Only valid players can guess")
  
  def hintCount: Int = 
    hints.size
  
  def reachedMaxHints: Boolean = 
    hintCount >= Rules.MaxHints

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
  availableEmojis: List[Emoji],
  myWrongGuesses: List[String]
):
  require(allScores.values.forall(_ >= 0), "All scores must be non-negative")
  require(hints.size <= Rules.MaxHints, s"Hints count exceeds maximum ${Rules.MaxHints}")
  require(remainingTimeMs >= 0, "Remaining time cannot be negative")
  require(secretWord.forall(_.nonEmpty), "Secret word cannot be empty if present")
  require(availableEmojis.distinct == availableEmojis, "Available emojis must be unique")
  require(hints.intersect(availableEmojis).isEmpty, 
    "Used hints must not appear in available emojis")
  
  // Phase-specific view invariants
  require(
    phase match
      case Phase.WaitingForHint | Phase.Guessing => 
        // Only describer sees word during game
        secretWord.isEmpty || amIDescriber
      case Phase.GameEnd => true // Everyone can see at game end
      case Phase.WaitingToStart => secretWord.isEmpty
    , "Secret word visibility violated"
  )
  
  require(
    // Only describer sees available emojis, and only when picking
    availableEmojis.isEmpty || (phase == Phase.WaitingForHint && amIDescriber),
    "Available emojis should only be visible to describer during WaitingForHint"
  )