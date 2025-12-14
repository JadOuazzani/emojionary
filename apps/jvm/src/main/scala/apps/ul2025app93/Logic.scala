package apps.ul2025app93

import cs214.webapp.*
import cs214.webapp.server.ClockDrivenStateMachine
import scala.util.{Try, Success, Failure}

/**
 * Game logic for Emojionary.
 * 
 * This class implements the [[cs214.webapp.server.ClockDrivenStateMachine]] interface,
 * providing the core game mechanics including state initialization, player view projection,
 * and event/tick handling.
 * 
 * == Game Flow ==
 * {{{
 * 1. init()      → Creates initial state with all players, scores at 0
 * 2. StartGame   → Transitions to WaitingForHint, first player becomes Describer
 * 3. SendFirstHint → Describer sets secret word + first emoji, starts timer
 * 4. Guessing    → Players guess, timer ticks every second
 * 5. SendHint    → If timer expires and hints < max, Describer adds emoji
 * 6. GameEnd     → Correct guess or max hints reached, shows winner
 * 7. Auto-restart → After GameEndDurationMs, winner becomes new Describer
 * }}}
 * 
 * == Design by Contract ==
 * All public methods use `require` for precondition validation and `ensuring`
 * for postcondition verification. State transitions are validated to maintain
 * game invariants.
 * 
 * == Thread Safety ==
 * The state machine is designed to be called from a single thread by the
 * webapp framework. All state mutations return new State objects.
 * 
 * @see [[State]] for the complete game state structure
 * @see [[View]] for the player-visible projection
 * @see [[Event]] for the events this logic handles
 */
class Logic extends ClockDrivenStateMachine[Event, State, View]:

  // ============ CONFIGURATION ============
  
  /**
   * Clock tick period in milliseconds.
   * The framework calls [[transition]] with a [[Tick]] event at this interval.
   */
  override val clockPeriodMs: Int = 1000
  
  /**
   * Wire protocol for serializing events and views.
   * @see [[Wire]] for the JSON encoding/decoding implementation
   */
  override val clockDrivenWire: AppWire[Event, View] = Wire
  
  /**
   * Application metadata displayed in the webapp interface.
   */
  override val appInfo: AppInfo = AppInfo(
    id = "ul2025app93",
    name = "Emojionary",
    description = "Guess the word using emojis!",
    year = 2025
  )

  // ============ EMOJI LIST ============
  
  /**
   * Complete list of available emojis for the game.
   * 
   * Contains 500+ emojis organized into 8 categories:
   *   - 😀 Faces & emotions
   *   - 👋 Gestures / hands
   *   - 🧍 People & roles
   *   - 🐶 Animals
   *   - 🌿 Nature & weather
   *   - 🍎 Food & drink
   *   - ⚽ Sports & activities
   *   - 🚗 Travel & places
   *   - 🧰 Objects
   *   - ❤️ Symbols
   * 
   * @note This is a `Vector` for O(1) random access and `contains` checks.
   */
  val AllEmojis: Vector[Emoji] = Vector(
    // 😀 Faces & emotions
    "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙",
    "😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤔","🤐","🤨","😐","😑","😶","🫥","😶‍🌫️","🙄","😏",
    "😣","😥","😮","🤐","😯","😪","😫","🥱","😴","😌","😛","😜","😝","🤤","😓","😔","😕","🫤","🙃",
    "😖","😞","😟","😤","😢","😭","😦","😧","😨","😩","😬","😰","😱","🥵","🥶","😳","🤯","😵","😵‍💫",
    "🥴","😲","🤠","🥳","😎","🤓","🧐","😒","😡","😠","🤬","😷","🤒","🤕","🤢","🤮","🤧","😇","🤥",
    "😈","👿","💀","☠️","👻","👽","🤖","💩",
    // 👋 Gestures / hands
    "👋","🤚","✋","🖐️","👌","🤌","🤏","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","👇","☝️","👍","👎",
    "✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏",
    // 🧍 People & roles
    "🧑","👧","👦","👩","👨","🧓","👶","🧒","🧑‍🎓","🧑‍🏫","🧑‍💻","🧑‍🔬","🧑‍🎨","🧑‍🚀","🧑‍🍳",
    "🧑‍⚕️","🧑‍🌾","🧑‍🔧","🧑‍🎤","🧑‍🎮","🧑‍✈️","🧑‍🚒","🧑‍⚖️","🕵️","💂","👷","🧑‍🏭",
    "🦸","🦹","🧙","🧝","🧛","🧟","🧞","🧜",
    // 🐶 Animals
    "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐻‍❄️","🐨","🐯","🦁","🐮","🐷","🐽","🐸","🐵","🙈","🙉","🙊",
    "🐔","🐧","🐦","🐤","🐣","🐥","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🪲","🐛","🦋","🐌",
    "🐞","🦂","🕷️","🕸️","🐢","🐍","🦎","🐙","🦑","🦐","🦞","🦀","🐠","🐟","🐡","🦈","🐬","🐳","🐋",
    "🦭","🐊","🦧","🦍","🐘","🦣","🦏","🦛","🦓","🦒","🦘","🐪","🐫","🦙","🦌","🐄","🐂","🐃","🐎",
    "🐖","🐏","🐑","🦝","🦨","🦡","🦫","🦦","🦥","🐁","🐀","🐿️","🦔","🐾",
    // 🌿 Nature & weather
    "🌵","🌴","🌲","🌳","🌱","🌿","☘️","🍀","🎍","🎋","🍃","🍂","🍁","🪴","🌸","🌼","🌻","🌺","🌹",
    "🥀","🌷","🌾","🌙","🌚","🌛","⭐","🌟","✨","⚡","☀️","🌤️","⛅","🌥️","☁️","🌦️","🌧️","⛈️","🌩️",
    "🌨️","❄️","☃️","⛄","💨","🌪️","🌫️","🌈","☔","💧","💦","🔥","🌊","🫧",
    // 🍎 Food & drink
    "🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆",
    "🥑","🥦","🥬","🥒","🌶️","🫑","🌽","🥕","🧄","🧅","🥔","🍠","🫘","🥜","🌰","🍞","🥐","🥖","🫓","🥨",
    "🥯","🥞","🧇","🧀","🍖","🍗","🥩","🥓","🍔","🍟","🍕","🌭","🥪","🌮","🌯","🫔","🥙","🥚","🍳","🥘",
    "🍲","🥣","🥗","🍿","🧈","🧂","🥫","🍝","🍜","🍜","🍛","🍣","🍱","🥟","🍤","🍙","🍚","🍘","🍥",
    "🦪","🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🥧","🍫","🍬","🍭","🍮","🍯",
    "☕","🫖","🍵","🥛","🧃","🥤","🧋","🍶","🍺","🍻","🥂","🍷","🍸","🍹","🧉","🍾",
    // ⚽ Sports & activities
    "⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🏓","🏸","🏒","🏑","🥍","🏏","⛳","🥊","🥋",
    "🎽","🛹","🛼","⛸️","🥌","🛷","🎿","🏂","🏋️","🤼","🤸","⛹️","🤺","🏊","🚣","🧗","🚴","🚵","🏇",
    "🧘","🏄","🏆","🥇","🥈","🥉","🎯","🪀","🪁","🎳","🎮","🕹️","🎲","♟️","🧩","🎨","🎭","🎤","🎧",
    "🎼","🎹","🥁","🪘","🎷","🎺","🎸","🪕","🎻",
    // 🚗 Travel & places
    "🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🛻","🚚","🚛","🚜","🛵","🏍️","🚲","🛴","🚨",
    "🚁","✈️","🛫","🛬","🛩️","🚀","🛸","🚂","🚆","🚄","🚅","🚈","🚇","🚊","🚉","🛶","⛵","🛥️","🚤",
    "🛳️","⚓","🗺️","🗽","🗼","🏰","🏯","🏟️","🏛️","⛪","🕌","🛕","🕍","⛩️","🗻","🏔️","⛰️","🌋","🏜️",
    "🏖️","🏝️","🏞️","🌅","🌄","🌆","🌇","🌃","🌉","🏙️",
    // 🧰 Objects
    "⌛","⏳","⏰","⏱️","📱","💻","🖥️","⌨️","🖱️","🖨️","📷","📸","🎥","📺","📡","🔋","🔌","💡","🔦",
    "🕯️","🧯","🧲","🧪","🧫","🧬","🔬","🔭","📡","📘","📗","📙","📕","📓","📒","📔","📚","📖","🔖",
    "📝","✏️","🖊️","🖋️","✒️","🖍️","📐","📏","🧮","📎","🖇️","📌","📍","✂️","🗃️","🗂️","🗄️",
    "🗑️","🔒","🔓","🔑","🗝️","🔨","🪓","⛏️","🧰","🔧","🪛","🔩","⚙️","🧱","🪜","🧹","🧺","🧻",
    "🧼","🪥","🧽","🛁","🚪","🪟","🪑","🛏️",
    // ❤️ Symbols
    "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❤️‍🔥","❤️‍🩹","💖","💗","💓","💞","💕","💘",
    "💯","✅","☑️","❌","❗","❓","‼️","⁉️","➕","➖","✖️","➗","🔁","🔄","🎵","🎶","🔔","🔕","📢","📣",
    "🟢","🟡","🟠","🔴","⚫","⚪","🟣","🟤","🔺","🔻","⭐","✨"
  )


  // ============ INIT ============
  
  /**
   * Initializes a new game with the given players.
   * 
   * Creates the initial game state with:
   *   - All players in the given order
   *   - First player (index 0) as the initial Describer
   *   - All scores set to 0
   *   - Phase set to `WaitingToStart`
   * 
   * @param clients the sequence of player IDs joining the game
   * @return the initial game state
   * @throws IllegalArgumentException if:
   *   - `clients` is empty
   *   - `clients` contains duplicate player IDs
   * 
   * @example
   * {{{
   * val logic = new Logic
   * val state = logic.init(Seq("player1", "player2", "player3"))
   * // state.phase == Phase.WaitingToStart
   * // state.scores == Map("player1" -> 0, "player2" -> 0, "player3" -> 0)
   * }}}
   */
  override def init(clients: Seq[UserId]): State =
    require(clients.nonEmpty, "Cannot start game with no players")
    require(clients.distinct.size == clients.size, "Duplicate player IDs not allowed")
    
    State(
      playerOrder = clients.toVector,
      describerIndex = 0,
      scores = clients.map(id => id -> 0).toMap,
      phase = Phase.WaitingToStart,
      secretWord = None,
      hints = List.empty,
      guesses = List.empty,
      currentHintGuessers = Set.empty,
      timerEndMs = 0L,
      winner = None
    ).ensuring { s =>
      s.playerOrder.size == clients.size &&
      s.scores.size == clients.size &&
      s.scores.values.forall(_ == 0) &&
      s.phase == Phase.WaitingToStart &&
      s.hints.isEmpty &&
      s.guesses.isEmpty &&
      s.secretWord.isEmpty &&
      s.winner.isEmpty
    }

  // ============ PROJECT ============
  
  /**
   * Projects the game state to a player-specific view.
   * 
   * Creates a [[View]] that hides information the player shouldn't see:
   *   - Secret word: Only visible to Describer during game, everyone at GameEnd
   *   - Available emojis: Only visible to Describer during WaitingForHint
   *   - Wrong guesses: Only shows the requesting player's own mistakes
   * 
   * @param state the current game state
   * @param userId the player requesting the view
   * @return the player-specific view of the game
   * @throws IllegalArgumentException if `userId` is not a player in the game
   * 
   * @example
   * {{{
   * val view = logic.project(state)("player1")
   * // If player1 is the Describer:
   * //   view.amIDescriber == true
   * //   view.secretWord == Some("scala")
   * //   view.availableEmojis == List("🔥", "💡", ...)
   * // If player1 is a guesser:
   * //   view.amIDescriber == false
   * //   view.secretWord == None
   * //   view.availableEmojis == Nil
   * }}}
   */
  override def project(state: State)(userId: UserId): View =
    require(state.playerOrder.contains(userId), s"Unknown player: $userId")
    
    val now = System.currentTimeMillis()
    val remaining = math.max(0L, state.timerEndMs - now)
    val amIDescriber = state.isDescriber(userId)
  
    // Describer sees word during game, everyone sees at GameEnd
    val visibleWord = state.phase match
      case Phase.WaitingForHint | Phase.Guessing if amIDescriber => state.secretWord
      case Phase.GameEnd => state.secretWord
      case _ => None
  
    // Only Describer sees available emojis, and only when picking
    val visibleEmojis = state.phase match
      case Phase.WaitingForHint if amIDescriber => availableEmojis(state)
      case _ => List.empty

    val allGuessesText: List[String] = state.guesses.map(_.text)

    // Errors history of THIS player only
    val myWrongGuesses: List[String] =
    state.guesses.collect {
      case GuessEntry(player, text, correct)
          if player == userId && !correct =>
        text
    }
  
    View(
      phase = state.phase,
      amIDescriber = amIDescriber,
      allScores = state.scores,
      hints = state.hints,
      guesses = allGuessesText,
      remainingTimeMs = remaining,
      secretWord = visibleWord,
      winner = state.winner,
      availableEmojis = visibleEmojis,
      myWrongGuesses = myWrongGuesses
    ).ensuring { v =>
      v.remainingTimeMs >= 0 &&
      v.hints.size <= Rules.MaxHints &&
      v.allScores.values.forall(_ >= 0) &&
      (v.amIDescriber || v.availableEmojis.isEmpty) &&
      v.myWrongGuesses.forall(g => !state.secretWord.contains(g.toLowerCase))
    }

  // ============ TRANSITION ============
  
  /**
   * Handles state transitions from events or clock ticks.
   * 
   * This is the main entry point for all game state changes. It dispatches
   * to [[handleTick]] for timer events or [[handleEvent]] for player actions.
   * 
   * @param state the current game state
   * @param userId the user ID associated with the event (system ID for ticks)
   * @param event either a `Tick` (from system clock) or an `Event` (from player)
   * @return a `Try` containing the list of actions to perform, or a `Failure`
   * @throws IllegalArgumentException (in Failure) if:
   *   - `userId` is not a player (for player events only)
   *   - The event is invalid for the current state
   * 
   * @note For `Tick` events, the `userId` is a system-generated ID (e.g.,
   *       "emojionary/.../clock") and is NOT validated against players.
   */
  override def transition(state: State)(userId: UserId, event: Either[Tick, Event]): Try[Seq[Action[State]]] =
    event match
      case Left(tick) => 
        // Tick events come from system clock, userId is not a player
        handleTick(state, tick)
      case Right(evt) => 
        require(state.playerOrder.contains(userId), s"Unknown player: $userId")
        handleEvent(state, userId, evt)

  // ============ TICK HANDLER ============
  
  /**
   * Handles clock tick events for timer-based transitions.
   * 
   * Manages automatic state transitions when timers expire:
   *   - `Guessing` → `GameEnd`: If timer expires and max hints reached
   *   - `Guessing` → `WaitingForHint`: If timer expires and more hints available
   *   - `GameEnd` → `WaitingForHint`: Auto-restart after display duration
   * 
   * @param state the current game state
   * @param tick the tick event with system timestamp
   * @return a `Try` containing the render action with potentially updated state
   * @throws IllegalArgumentException (in Failure) if tick timestamp is negative
   */
  private def handleTick(state: State, tick: Tick): Try[Seq[Action[State]]] =
    require(tick.systemMillis >= 0, "Invalid tick timestamp")
    
    state.phase match
      case Phase.Guessing if tick.systemMillis >= state.timerEndMs =>
        // Timer expired
        if state.hintCount >= Rules.MaxHints then
          // Max hints reached, game over with no winner
          val newState = state.copy(
            phase = Phase.GameEnd,
            timerEndMs = tick.systemMillis + Rules.GameEndDurationMs
          ).ensuring(s => s.phase == Phase.GameEnd && s.winner.isEmpty)
          Success(Seq(Action.Render(newState)))
        else
          // Go to next hint
          val newState = state.copy(phase = Phase.WaitingForHint)
            .ensuring(_.phase == Phase.WaitingForHint)
          Success(Seq(Action.Render(newState)))
      
      case Phase.GameEnd if tick.systemMillis >= state.timerEndMs =>
        // Auto-continue to new game (Timer-based)
        val newState = startNewRound(state)
        Success(Seq(Action.Render(newState)))
      
      case _ =>
        Success(Seq(Action.Render(state)))

  // ============ EVENT HANDLERS ============
  
  /**
   * Dispatches player events to their specific handlers.
   * 
   * @param state the current game state
   * @param userId the player who sent the event
   * @param event the event to handle
   * @return a `Try` containing the actions to perform
   */
  private def handleEvent(state: State, userId: UserId, event: Event): Try[Seq[Action[State]]] =
    event match
      case Event.StartGame =>
        handleStartGame(state, userId)
      case Event.SendFirstHint(secretWord, emoji) =>
        handleSendFirstHint(state, userId, secretWord, emoji)
      case Event.SendHint(emoji) =>
        handleSendHint(state, userId, emoji)
      case Event.SubmitGuess(text) =>
        handleSubmitGuess(state, userId, text)

  /**
   * Handles the StartGame event.
   * 
   * Transitions from `WaitingToStart` to `WaitingForHint`, or restarts
   * from `GameEnd` before the auto-restart timer.
   * 
   * @param state the current game state
   * @param userId the player requesting to start (any player can start)
   * @return Success with render action, or Failure if:
   *   - Less than 2 players (from WaitingToStart)
   *   - Game already in progress (Guessing or WaitingForHint)
   */
  private def handleStartGame(state: State, userId: UserId): Try[Seq[Action[State]]] =
    if state.phase == Phase.WaitingToStart then
        if !state.canStart then Failure(IllegalStateException("Need at least 2 players"))
        else
            val newState = state.copy(phase = Phase.WaitingForHint)
              .ensuring(_.phase == Phase.WaitingForHint)
            Success(Seq(Action.Render(newState)))
    else if state.phase == Phase.GameEnd then
        // Manual restart via "Play Again" button before timer expires
        val newState = startNewRound(state)
        Success(Seq(Action.Render(newState)))
    else
        Failure(IllegalStateException("Game already started"))

  /**
   * Starts a new round, resetting round-specific state.
   * 
   * Used both by the auto-restart timer and the manual "Play Again" button.
   * The winner of the previous round becomes the new Describer. If there
   * was no winner, the Describer rotates to the next player.
   * 
   * @param state the current game state (must be in GameEnd or have players)
   * @return a fresh state for the new round
   * @throws IllegalArgumentException if player order is empty
   */
  private def startNewRound(state: State): State =
    require(state.playerOrder.nonEmpty, "Cannot start round with no players")
    
    val newDescriberIdx = nextDescriberIndex(state, state.winner)
    State(
        playerOrder = state.playerOrder,
        describerIndex = newDescriberIdx,
        scores = state.scores,
        phase = Phase.WaitingForHint,
        secretWord = None,
        hints = List.empty,
        guesses = List.empty,
        currentHintGuessers = Set.empty,
        timerEndMs = 0L,
        winner = None
    ).ensuring { s =>
      s.playerOrder == state.playerOrder &&
      s.scores == state.scores &&
      s.phase == Phase.WaitingForHint &&
      s.hints.isEmpty &&
      s.guesses.isEmpty &&
      s.secretWord.isEmpty &&
      s.winner.isEmpty &&
      s.describerIndex >= 0 &&
      s.describerIndex < s.playerOrder.size
    }

  /**
   * Handles the first hint of a round, setting the secret word.
   * 
   * Transitions from `WaitingForHint` to `Guessing` and starts the timer.
   * 
   * @param state the current game state
   * @param userId the player sending the hint (must be Describer)
   * @param secretWord the word that guessers must guess
   * @param emoji the first emoji hint
   * @return Success with render action, or Failure if:
   *   - Not in WaitingForHint phase
   *   - User is not the Describer
   *   - Hints already exist (use SendHint instead)
   *   - Emoji is not in [[AllEmojis]]
   *   - Secret word is empty
   */
  private def handleSendFirstHint(state: State, userId: UserId, secretWord: Word, emoji: Emoji): Try[Seq[Action[State]]] =
    require(secretWord != null, "Secret word cannot be null")
    require(emoji != null, "Emoji cannot be null")
    
    if state.phase != Phase.WaitingForHint then
      Failure(IllegalStateException("Not waiting for hint"))
    else if !state.isDescriber(userId) then
      Failure(IllegalStateException("Only Describer can send hints"))
    else if state.hints.nonEmpty then
      Failure(IllegalStateException("Use SendHint for subsequent hints"))
    else if !AllEmojis.contains(emoji) then
      Failure(IllegalArgumentException("Invalid emoji"))
    else if secretWord.trim.isEmpty then
      Failure(IllegalArgumentException("Secret word cannot be empty"))
    else
      val now = System.currentTimeMillis()
      val newState = state.copy(
        phase = Phase.Guessing,
        secretWord = Some(secretWord.trim.toLowerCase),
        hints = List(emoji),
        currentHintGuessers = Set.empty,
        timerEndMs = now + Rules.HintDurationMs
      ).ensuring { s =>
        s.phase == Phase.Guessing &&
        s.hints.size == 1 &&
        s.secretWord.isDefined &&
        s.currentHintGuessers.isEmpty
      }
      Success(Seq(Action.Render(newState)))

  /**
   * Handles subsequent hints after the first.
   * 
   * Transitions from `WaitingForHint` to `Guessing` and resets the timer.
   * 
   * @param state the current game state
   * @param userId the player sending the hint (must be Describer)
   * @param emoji the emoji to add as a hint
   * @return Success with render action, or Failure if:
   *   - Not in WaitingForHint phase
   *   - User is not the Describer
   *   - No hints exist yet (use SendFirstHint instead)
   *   - Emoji is not in [[AllEmojis]]
   *   - Emoji was already used as a hint
   */
  private def handleSendHint(state: State, userId: UserId, emoji: Emoji): Try[Seq[Action[State]]] =
    require(emoji != null, "Emoji cannot be null")
    
    if state.phase != Phase.WaitingForHint then
      Failure(IllegalStateException("Not waiting for hint"))
    else if !state.isDescriber(userId) then
      Failure(IllegalStateException("Only Describer can send hints"))
    else if state.hints.isEmpty then
      Failure(IllegalStateException("Use SendFirstHint for first hint"))
    else if !AllEmojis.contains(emoji) then
      Failure(IllegalArgumentException("Invalid emoji"))
    else if state.hints.contains(emoji) then
      Failure(IllegalArgumentException("Emoji already used"))
    else
      val now = System.currentTimeMillis()
      val newState = state.copy(
        phase = Phase.Guessing,
        hints = state.hints :+ emoji,
        currentHintGuessers = Set.empty,
        timerEndMs = now + Rules.HintDurationMs
      ).ensuring { s =>
        s.phase == Phase.Guessing &&
        s.hints.size == state.hints.size + 1 &&
        s.hints.last == emoji &&
        s.currentHintGuessers.isEmpty
      }
      Success(Seq(Action.Render(newState)))

  /**
   * Handles a guess submission from a player.
   * 
   * == Correct Guess ==
   * Transitions to `GameEnd`, awards points, and records the winner.
   * Points = MaxHints - hintCount + 1 (fewer hints = more points).
   * 
   * == Wrong Guess ==
   * Records the guess and tracks that this player has guessed.
   * If all guessers have guessed wrong:
   *   - If max hints reached → `GameEnd` with no winner
   *   - Otherwise → `WaitingForHint` for next emoji
   * 
   * @param state the current game state
   * @param userId the player submitting the guess (must not be Describer)
   * @param text the guessed word
   * @return Success with render action, or Failure if:
   *   - Not in Guessing phase
   *   - User is the Describer (Describers cannot guess)
   */
  private def handleSubmitGuess(state: State, userId: UserId, text: String): Try[Seq[Action[State]]] =
    require(text != null, "Guess text cannot be null")
    
    if state.phase != Phase.Guessing then
      Failure(IllegalStateException("Not in guessing phase"))
    else if !state.isGuesser(userId) then
      Failure(IllegalStateException("Describer cannot guess"))
    else
      val normalizedGuess = text.trim.toLowerCase
      val isCorrect = state.secretWord.exists(_ == normalizedGuess)
      
      val entry = GuessEntry(
        player = userId,
        text = text.trim,
        correct = isCorrect
      )

      if isCorrect then
        val points = calculatePoints(state.hintCount)
        val newScores = state.scores.updated(userId, state.scores.getOrElse(userId, 0) + points)
        val now = System.currentTimeMillis()
        val newState = state.copy(
          phase = Phase.GameEnd,
          guesses = state.guesses :+ entry,
          scores = newScores,
          winner = Some(userId),
          timerEndMs = now + Rules.GameEndDurationMs
        ).ensuring { s =>
          s.phase == Phase.GameEnd &&
          s.winner.contains(userId) &&
          s.scores(userId) > state.scores(userId) &&
          s.guesses.size == state.guesses.size + 1
        }
        Success(Seq(Action.Render(newState)))
      else
        val allGuessers: Set[UserId] = 
          state.playerOrder.filterNot(uid => state.isDescriber(uid)).toSet

        val updatedGuessers: Set[UserId] = 
          state.currentHintGuessers + userId

        // Have all guessers attempted for this hint?
        val everyoneAnswered = updatedGuessers == allGuessers

        val now = System.currentTimeMillis()

        val newState = 
          if everyoneAnswered then
            // If at max hints → game over with no winner
            if state.hintCount >= Rules.MaxHints then
              state.copy(
                phase = Phase.GameEnd,
                guesses = state.guesses :+ entry,
                currentHintGuessers = updatedGuessers,
                winner = None,
                timerEndMs = now + Rules.GameEndDurationMs
              ).ensuring { s =>
                s.phase == Phase.GameEnd &&
                s.winner.isEmpty &&
                s.guesses.size == state.guesses.size + 1
              }
            else
              // Otherwise, go back to WaitingForHint for another emoji
              state.copy(
                phase = Phase.WaitingForHint,
                guesses = state.guesses :+ entry,
                currentHintGuessers = updatedGuessers,
                timerEndMs = 0L
              ).ensuring { s =>
                s.phase == Phase.WaitingForHint &&
                s.guesses.size == state.guesses.size + 1
              }
          else
            // Still waiting for other players to guess
            state.copy(
              guesses = state.guesses :+ entry,
              currentHintGuessers = updatedGuessers
            ).ensuring { s =>
              s.phase == Phase.Guessing &&
              s.guesses.size == state.guesses.size + 1 &&
              s.currentHintGuessers.contains(userId)
            }
        Success(Seq(Action.Render(newState)))

  // ============ HELPERS ============
  
  /**
   * Returns the list of emojis that haven't been used as hints yet.
   * 
   * @param state the current game state
   * @return list of available emojis (all emojis minus those in hints)
   * @throws IllegalArgumentException if hints list is null
   */
  private def availableEmojis(state: State): List[Emoji] =
    require(state.hints != null, "Hints list cannot be null")
    AllEmojis.filterNot(state.hints.contains).distinct.toList
      .ensuring(result => result.forall(e => !state.hints.contains(e)))
  
  /**
   * Calculates the next Describer index for a new round.
   * 
   * If there was a winner, they become the next Describer.
   * Otherwise, the Describer rotates to the next player in order.
   * 
   * @param state the current game state
   * @param winner the winner of the previous round, if any
   * @return the index of the next Describer in playerOrder
   * @throws IllegalArgumentException if:
   *   - Player order is empty
   *   - Winner is not in player order (if Some)
   */
  private def nextDescriberIndex(state: State, winner: Option[UserId]): Int =
    require(state.playerOrder.nonEmpty, "Player order cannot be empty")
    
    val result = winner match
      case Some(winnerId) =>
        val idx = state.playerOrder.indexOf(winnerId)
        require(idx >= 0, s"Winner $winnerId not found in player order")
        idx
      case None =>
        (state.describerIndex + 1) % state.playerOrder.size
    result.ensuring(idx => idx >= 0 && idx < state.playerOrder.size)
  
  /**
   * Calculates points awarded for a correct guess.
   * 
   * Points decrease with more hints used:
   * | Hints | Points |
   * |-------|--------|
   * | 1     | 5      |
   * | 2     | 4      |
   * | 3     | 3      |
   * | 4     | 2      |
   * | 5     | 1      |
   * 
   * @param hintCount the number of hints used (1 to MaxHints)
   * @return the points to award (MaxHints - hintCount + 1)
   * @throws IllegalArgumentException if hintCount is not in valid range
   */
  private def calculatePoints(hintCount: Int): Int =
    require(hintCount >= 1, s"Hint count must be at least 1, got $hintCount")
    require(hintCount <= Rules.MaxHints, s"Hint count cannot exceed ${Rules.MaxHints}, got $hintCount")
    (Rules.MaxHints - hintCount + 1).ensuring(pts => pts >= 1 && pts <= Rules.MaxHints)