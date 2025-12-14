package apps.emojionary

import cs214.webapp.UserId

/**
 * Type alias for emoji strings.
 * 
 * Emojis are represented as Unicode strings (e.g., "🔥", "💡", "🎯").
 * The game supports 500+ emojis across 8 categories including faces,
 * gestures, animals, nature, food, sports, travel, and symbols.
 */
type Emoji = String

/**
 * Type alias for secret words.
 * 
 * Words are stored in lowercase, trimmed format for consistent comparison.
 */
type Word = String

/**
 * Represents a single guess attempt by a player.
 * 
 * @param player the ID of the player who made the guess
 * @param text the guessed word (trimmed but preserves original case for display)
 * @param correct whether the guess matched the secret word
 * 
 * @throws IllegalArgumentException if player ID is empty or guess text is blank
 * 
 * @example
 * {{{
 * val guess = GuessEntry("player1", "scala", correct = true)
 * val wrongGuess = GuessEntry("player2", "java", correct = false)
 * }}}
 */
case class GuessEntry(
  player: UserId,
  text: String,
  correct: Boolean
):
  require(player.nonEmpty, "Player ID cannot be empty")
  require(text.trim.nonEmpty, "Guess text cannot be empty")

/**
 * Game configuration constants.
 * 
 * These values define the core game rules and timing parameters.
 * All constants are validated at class load time to ensure positive values.
 * 
 * == Game Balance ==
 * The scoring system rewards quick guessing:
 * - 1 hint = 5 points
 * - 2 hints = 4 points
 * - 3 hints = 3 points
 * - 4 hints = 2 points
 * - 5 hints = 1 point
 */
object Rules:
  /**
   * Maximum number of emoji hints allowed per round.
   * After this many hints, if no one guesses correctly, the round ends.
   */
  val MaxHints: Int = 5
  
  /**
   * Duration in milliseconds for each hint phase.
   * Players have this much time to guess after each new hint.
   */
  val HintDurationMs: Long = 25_000
  
  /**
   * Duration in milliseconds to display the game end screen.
   * After this time, the next round starts automatically.
   */
  val GameEndDurationMs: Long = 20_000

  // Validate constants at load time
  require(MaxHints > 0, "MaxHints must be positive")
  require(HintDurationMs > 0, "HintDurationMs must be positive")
  require(GameEndDurationMs > 0, "GameEndDurationMs must be positive")

/**
 * Represents the current phase of the game.
 * 
 * The game follows a state machine pattern with the following transitions:
 * {{{
 * WaitingToStart ──StartGame──> WaitingForHint ──SendFirstHint──> Guessing
 *                                     ↑                              │
 *                                     │                              │
 *                               (timer expires                 (correct guess)
 *                                & hints < max)                      │
 *                                     │                              ↓
 *                                     └────────────────────────── GameEnd
 *                                          (auto-restart)
 * }}}
 */
enum Phase:
  /**
   * Initial phase before the game starts.
   * Players are joining and waiting for someone to click "Start Game".
   */
  case WaitingToStart
  
  /**
   * The Describer is choosing the next emoji hint.
   * No timer is active during this phase.
   */
  case WaitingForHint
  
  /**
   * Active guessing phase.
   * Guessers try to guess the word before the timer expires.
   */
  case Guessing
  
  /**
   * Round has ended (either someone guessed correctly or max hints reached).
   * Shows the results and winner, then auto-transitions to next round.
   */
  case GameEnd

/**
 * Events that players can send to the server.
 * 
 * These represent all possible player actions in the game.
 * Events are serialized to JSON by [[Wire.eventFormat]] for transmission.
 */
enum Event:
  /**
   * Request to start the game.
   * Can be sent during `WaitingToStart` or `GameEnd` phases.
   * Requires at least 2 players to succeed.
   */
  case StartGame
  
  /**
   * Send the first hint of a round, along with the secret word.
   * 
   * @param secretWord the word that guessers must guess
   * @param emoji the first emoji hint
   * 
   * @note Only the Describer can send this, and only during `WaitingForHint`
   *       when no hints have been sent yet.
   */
  case SendFirstHint(secretWord: Word, emoji: Emoji)
  
  /**
   * Send an additional emoji hint.
   * 
   * @param emoji the emoji to add as a hint
   * 
   * @note Only the Describer can send this, and only during `WaitingForHint`
   *       after the first hint has been sent. The emoji must not have been
   *       used already in this round.
   */
  case SendHint(emoji: Emoji)
  
  /**
   * Submit a guess for the secret word.
   * 
   * @param text the guessed word
   * 
   * @note Only guessers (non-Describers) can submit guesses,
   *       and only during the `Guessing` phase.
   */
  case SubmitGuess(text: String)

/**
 * Complete game state.
 * 
 * This is the authoritative state stored on the server. It contains all
 * information needed to determine valid actions and compute player views.
 * 
 * == Design by Contract ==
 * The State class enforces numerous invariants via `require` statements:
 *   - Player order must be non-empty with no duplicates
 *   - Describer index must be within bounds
 *   - Scores must match player order exactly
 *   - All scores must be non-negative
 *   - Hints cannot exceed MaxHints and must be unique
 *   - Secret word must be trimmed, lowercase, and non-empty
 *   - Phase-specific constraints (e.g., Guessing requires secretWord)
 * 
 * @param playerOrder ordered list of player IDs (first player is index 0)
 * @param describerIndex index into playerOrder for the current Describer
 * @param scores current scores for all players
 * @param phase current game phase
 * @param secretWord the word to guess (None before first hint)
 * @param hints list of emoji hints given so far (in order)
 * @param guesses list of all guess attempts this round
 * @param currentHintGuessers players who have guessed for the current hint
 * @param timerEndMs system time when the current timer expires
 * @param winner player who won the round (None if no winner yet)
 * 
 * @throws IllegalArgumentException if any invariant is violated
 * 
 * @see [[View]] for the player-visible projection of this state
 */
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
  // ============ INVARIANTS ============
  
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

  // ============ DERIVED PROPERTIES ============

  /**
   * Returns the current Describer's user ID.
   * 
   * @return the user ID of the player who is describing
   */
  def describer: UserId = 
    playerOrder(describerIndex)
      .ensuring(playerOrder.contains(_), "Describer must be in player order")
  
  /**
   * Checks if a user is the current Describer.
   * 
   * @param uid the user ID to check
   * @return true if the user is the Describer, false otherwise
   */
  def isDescriber(uid: UserId): Boolean = 
    uid == describer
  
  /**
   * Checks if a user is a guesser (not the Describer).
   * 
   * @param uid the user ID to check
   * @return true if the user is a guesser, false if they are the Describer
   */
  def isGuesser(uid: UserId): Boolean = 
    !isDescriber(uid)
  
  /**
   * Checks if the game has enough players to start.
   * 
   * @return true if there are at least 2 players
   */
  def canStart: Boolean = 
    playerOrder.size >= 2
  
  /**
   * Checks if a user can send a hint.
   * 
   * A user can send a hint if:
   *   - The phase is `WaitingForHint`
   *   - The user is the Describer
   * 
   * @param uid the user ID to check
   * @return true if the user can send a hint
   */
  def canSendHint(uid: UserId): Boolean =
    (phase == Phase.WaitingForHint && isDescriber(uid))
      .ensuring(result => !result || playerOrder.contains(uid), 
        "Only valid players can send hints")
  
  /**
   * Checks if a user can submit a guess.
   * 
   * A user can guess if:
   *   - The phase is `Guessing`
   *   - The user is NOT the Describer
   * 
   * @param uid the user ID to check
   * @return true if the user can submit a guess
   */
  def canGuess(uid: UserId): Boolean =
    (phase == Phase.Guessing && isGuesser(uid))
      .ensuring(result => !result || playerOrder.contains(uid),
        "Only valid players can guess")
  
  /**
   * Returns the number of hints given so far.
   * 
   * @return the count of emoji hints (0 to MaxHints)
   */
  def hintCount: Int = 
    hints.size
  
  /**
   * Checks if the maximum number of hints has been reached.
   * 
   * @return true if no more hints can be given
   */
  def reachedMaxHints: Boolean = 
    hintCount >= Rules.MaxHints

/**
 * Player-specific view of the game state.
 * 
 * This is what gets sent to each player. It hides information that
 * players shouldn't see (e.g., guessers can't see the secret word).
 * 
 * == Information Hiding ==
 * - `secretWord`: Only visible to Describer during game, everyone at GameEnd
 * - `availableEmojis`: Only visible to Describer during WaitingForHint
 * - `myWrongGuesses`: Only contains the current player's wrong guesses
 * 
 * @param phase current game phase
 * @param amIDescriber whether the receiving player is the Describer
 * @param allScores scores for all players (public information)
 * @param hints emoji hints given so far
 * @param guesses all guesses made this round (text only, no player info)
 * @param remainingTimeMs milliseconds until timer expires
 * @param secretWord the secret word (None if hidden from this player)
 * @param winner the round winner (None if no winner yet)
 * @param availableEmojis emojis the Describer can choose from
 * @param myWrongGuesses this player's incorrect guesses
 * 
 * @throws IllegalArgumentException if any invariant is violated
 * 
 * @see [[State]] for the complete server-side state
 * @see [[Logic.project]] for how Views are created from State
 */
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
  // ============ INVARIANTS ============
  
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